package net.kino2718.podcast.ui.main

import android.app.Application
import android.content.ComponentName
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PlaylistItem
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.service.PlaybackService
import net.kino2718.podcast.ui.utils.ObservePlaybackStates
import net.kino2718.podcast.ui.utils.getExtensionFromUrl
import net.kino2718.podcast.utils.MyLog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
    private val repo = Repository(appContext)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var _audioPlayerFlow = MutableStateFlow<Player?>(null)
    val audioPlayerFlow = _audioPlayerFlow.asStateFlow()

    private val appStatesFlow = repo.getAppStatesFlow()
    val showControl = appStatesFlow
        .map { state ->
            state?.channelId != null && state.episodeId != null
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val speedFlow = appStatesFlow
        .map { it?.speed ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.Lazily, 1.0f)

    private val _playItemFlow = MutableStateFlow<PlayItem?>(null)

    init {
        viewModelScope.launch {
            // 古いダウンロードファイルの削除
            repo.deleteOldDownloadFiles()
            // playerの初期化
            initializePlayer()
            // この段階では_audioPlayerFlowは上記関数によって設定されている
            initializeController()
            // playlistの監視
            observePlaylist()
        }
    }

    private suspend fun initializePlayer() {
        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        val player = future.await()

        repo.getAppStates()?.speed?.let { player.setPlaybackSpeed(it) }

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                MyLog.e(TAG, "onPlayerError: $error")
            }
        })

        controllerFuture = future
        _audioPlayerFlow.value = player

        // observeは戻ってこないのでlaunchで別コルーチンとする
        viewModelScope.launch {
            ObservePlaybackStates().observe(
                player = player,
                scope = viewModelScope,
                onChanged = { index, position, duration ->
                    if (duration != C.TIME_UNSET) {
                        currentPlayItemList?.getOrNull(index)?.let { currentPlayItem ->
                            if (currentPlayItemIndex != index) {
                                repo.setCurrentPlayItem(currentPlayItem)
                                currentPlayItemIndex = index
                            }
                            val episode = currentPlayItem.episode
                            val completed = abs(duration - position) < 2000 // 終了まで2秒以内なら再生完了とする
                            repo.updatePlaybackInfos(
                                episode.id, position, duration, completed, Instant.now()
                            )
                        }
                    }
                },
                onPlayingChanged = { playing ->
                    if (playlistChanged && !playing) {
                        setPlaylist(repo.getPlaylistItems())
                        playlistChanged = false
                    }
                },
            )
        }
    }

    private suspend fun initializeController() {
        repo.getAppStates()?.let { playItemId ->
            val channelId = playItemId.channelId
            val episodeId = playItemId.episodeId
            if (channelId != null && episodeId != null) {
                val channel = repo.getChannelById(channelId)
                val episode = repo.getEpisodeById(episodeId)
                if (channel != null && episode != null) {
                    val playItem = PlayItem(
                        channel = channel,
                        episode = episode,
                        inPlaylist = playItemId.inPlaylist
                    )
                    _playItemFlow.value = playItem
                    if (playItemId.inPlaylist) {
                        val (playItemList, startIndex) = getPlayItemListAndStartIndex(
                            repo.getPlaylistItems(),
                            episode.id
                        )
                        if (playItemList.isNotEmpty()) {
                            setPlayer(playItemList, startIndex, false)
                        } else {
                            setPlayer(listOf(playItem), 0, false)
                        }
                    } else {
                        setPlayer(listOf(playItem), 0, false)
                    }
                }
            }
        }
    }

    // playlistが変更されまだ反映されていないことを示すフラグ
    private var playlistChanged = false

    private fun observePlaylist() {
        viewModelScope.launch {
            repo.getPlaylistItemsFlow().collect { playlistItems ->
                // playerが再生中でなければplaylistを更新する
                _audioPlayerFlow.value?.let { player ->
                    if (!player.isPlaying) {
                        setPlaylist(playlistItems)
                        playlistChanged = false
                    } else {
                        playlistChanged = true
                    }
                }
            }
        }
    }

    private suspend fun setPlaylist(playlistItems: List<PlaylistItem>) {
        repo.getAppStates()?.let { playItemId ->
            val episodeId = playItemId.episodeId
            if (playItemId.inPlaylist && episodeId != null) {
                val (playItemList, startIndex) = getPlayItemListAndStartIndex(
                    playlistItems,
                    episodeId
                )
                if (playItemList.isNotEmpty()) {
                    setPlayer(playItemList, startIndex, false)
                }
            }
        }
    }

    private suspend fun getPlayItemListAndStartIndex(
        playlistItems: List<PlaylistItem>,
        startEpisodeId: Long
    ): Pair<List<PlayItem>, Int> {
        // channel.idとepisode.idのPlayListをPlayItemのlistに変換する。
        val playItemList = playlistItems.mapNotNull {
            val channel = repo.getChannelById(it.channelId)
            val episode = repo.getEpisodeById(it.episodeId)
            if (channel != null && episode != null) {
                PlayItem(channel = channel, episode = episode, true)
            } else null
        }
        // 再生開始位置を検索する。
        val start = if (playItemList.isNotEmpty()) {
            playItemList.indexOfFirst {
                it.episode.id == startEpisodeId
            }.coerceAtLeast(0) // 見つからない場合は0
        } else {
            // listが空
            -1
        }
        return Pair(playItemList, start)
    }

    fun setPlayItem(playItem: PlayItem) {
        viewModelScope.launch {
            val playItemWithId = repo.setCurrentPlayItem(playItem)
            _playItemFlow.value = playItemWithId
            setPlayer(listOf(playItemWithId), 0, true)
        }
    }

    fun setPlayItems(playItemList: List<PlayItem>, startIndex: Int) {
        viewModelScope.launch {
            val startPlayItem = playItemList[startIndex]
            val playItemWithId = repo.setCurrentPlayItem(startPlayItem)
            _playItemFlow.value = playItemWithId
            setPlayer(playItemList, startIndex, true)
        }
    }

    fun addToPlaylist(playItem: PlayItem) {
        viewModelScope.launch {
            repo.addToPlaylist(playItem)
        }
    }

    private val downloadMap: MutableMap<String, Boolean> = mutableMapOf()
    private val downloadMutex = Mutex()

    fun download(playItem: PlayItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // 既にこのファイルをdownload中だったら何もしない
            downloadMutex.withLock {
                val inDownloading = downloadMap[playItem.episode.guid] ?: false
                if (inDownloading) return@launch
                downloadMap[playItem.episode.guid] = true
            }

            val playItem1 = repo.upsertPlayItem(playItem) // idが割り振られていることを確定する
            val episode = playItem1.episode
            // urlから拡張子を取得する
            episode.url.getExtensionFromUrl()?.let { ext ->
                // episode idからファイル名を作成
                val fName = "${episode.id}.$ext"
                val destFile = File(appContext.filesDir, fName)
                if (downloadFile(episode.url, destFile)) {
                    repo.updateDownloadFile(episode.id, destFile.absolutePath)
                }
            }
            downloadMutex.withLock {
                // download終了
                downloadMap[playItem.episode.guid] = false
            }
        }
    }

    private fun downloadFile(url: String, destFile: File): Boolean {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                MyLog.e(TAG, "Failed to download file: ${response.code}")
                return false
            }

            val body = response.body
            try {
                // バッファを使ってストリームから直接読み込み、ファイルへ書き出す
                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } >= 0) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                return true
            } catch (e: IOException) {
                MyLog.e(TAG, "Error saving file: ${e.message}")
                return false
            }
        }
    }

    private var currentPlayItemIndex = -1
    private var currentPlayItemList: List<PlayItem>? = null

    private fun setPlayer(playItemList: List<PlayItem>, startIndex: Int, autoPlay: Boolean) {
        viewModelScope.launch {
            currentPlayItemIndex = startIndex
            currentPlayItemList = playItemList

            val startEpisode = playItemList[startIndex].episode
            _audioPlayerFlow.value?.let { player ->
                // posの値はdurationよりある程度小さくしないとExoPlayerから返ってくるdurationの値が異常に小さくなる
                val pos = startEpisode.playbackPosition.coerceIn(
                    0L, max(0L, startEpisode.duration - 100L)
                )
                val mediaItemList = playItemList.mapIndexed { i, playItem ->
                    val channel = playItem.channel
                    val episode = playItem.episode
                    val metadata = MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setSubtitle(channel.title)
                        .setArtist(episode.author)
                        .setTrackNumber(i)
                        .setTotalTrackCount(playItemList.size)
                        .setArtworkUri(episode.imageUrl?.toUri())
                        .build()
                    val downloadFile = episode.getDownloadFileUri()
                    // databaseにはdownload fileがあるのに実際のファイルが無い場合はdatabaseを変更する
                    if (episode.downloadFile != null && downloadFile == null)
                        repo.updateDownloadFile(episode.id, null)
                    val url = downloadFile ?: episode.url
                    MediaItem.Builder()
                        .setUri(url)
                        .setMediaMetadata(metadata)
                        .build()
                }
                player.setMediaItems(mediaItemList, startIndex, pos)
                player.prepare()
                if (autoPlay) player.play() else player.pause()
            }
        }
    }

    private fun releasePlayer() {
        _audioPlayerFlow.value?.stop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        _audioPlayerFlow.value = null
    }

    private fun Episode.getDownloadFileUri(): String? {
        return downloadFile?.let {
            val file = File(it)
            if (file.exists() && file.isFile) downloadFile.toUri().path
            else null
        }
    }

    fun setSpeed(speed: Float) {
        _audioPlayerFlow.value?.setPlaybackSpeed(speed)
        viewModelScope.launch { repo.updateSpeed(speed) }
    }

    fun dismissPlayer() {
        viewModelScope.launch {
            repo.clearCurrentPlayItem()
        }
    }

    override fun onCleared() {
        releasePlayer()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
