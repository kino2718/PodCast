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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.service.PlaybackService
import net.kino2718.podcast.ui.utils.ObservePlaybackPosition
import net.kino2718.podcast.utils.MyLog
import kotlin.math.abs
import kotlin.math.max

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
    private val repo = Repository(appContext)

    private val lastPlayedItemIdFlow = repo.getLastPlayedItemIdFlow()
    val showControl = lastPlayedItemIdFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _playItemFlow = MutableStateFlow<PlayItem?>(null)

    init {
        viewModelScope.launch {
            lastPlayedItemIdFlow.first()?.let { playItemId ->
                val channel = repo.getChannelById(playItemId.channelId)
                val item = repo.getEpisodeById(playItemId.episodeId)
                if (channel != null && item != null) {
                    val playItem =
                        PlayItem(channel = channel, episode = item, playItemId.inPlaylist)
                    MyLog.d(TAG, "inPlaylist = ${playItemId.inPlaylist}")
                    _playItemFlow.value = playItem
                    if (playItemId.inPlaylist) {
                        repo.getPlaylistItems()
                        val playItemList = repo.getPlaylistItems().mapNotNull {
                            val channel = repo.getChannelById(it.channelId)
                            val episode = repo.getEpisodeById(it.episodeId)
                            if (channel != null && episode != null) {
                                PlayItem(channel = channel, episode = episode, true)
                            } else null
                        }
                        if (playItemList.isNotEmpty()) {
                            val startIndex = playItemList.indexOfFirst {
                                it.episode.id == item.id
                            }.coerceAtLeast(0)
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

    fun setPlayItem(playItem: PlayItem) {
        viewModelScope.launch {
            val playItemWithId = addPlayItem(playItem)
            _playItemFlow.value = playItemWithId
            setPlayer(listOf(playItemWithId), 0, true)
        }
    }

    fun setPlayItems(playItemList: List<PlayItem>, startIndex: Int) {
        viewModelScope.launch {
            val startPlayItem = playItemList[startIndex]
            val playItemWithId = addPlayItem(startPlayItem)
            _playItemFlow.value = playItemWithId
            setPlayer(playItemList, startIndex, true)
        }
    }

    private suspend fun addPlayItem(playItem: PlayItem): PlayItem {
        return repo.addPlayItem(playItem)
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var _audioPlayerFlow = MutableStateFlow<Player?>(null)
    val audioPlayerFlow = _audioPlayerFlow.asStateFlow()

    init {
        viewModelScope.launch {
            initializePlayer()
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
                    MediaItem.Builder()
                        .setUri(episode.url)
                        .setMediaMetadata(metadata)
                        .build()
                }
                player.setMediaItems(mediaItemList, startIndex, pos)
                player.prepare()
                if (autoPlay) player.play() else player.pause()
            }
        }
    }

    private suspend fun initializePlayer() {
        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        val player = future.await()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                MyLog.d(TAG, "uri: ${player.currentMediaItem?.localConfiguration?.uri}")
                MyLog.d(TAG, "tag: ${player.currentMediaItem?.localConfiguration?.tag}")
            }
        })

        controllerFuture = future
        _audioPlayerFlow.value = player

        ObservePlaybackPosition().observe(
            player = player,
            scope = viewModelScope,
            onChanged = { index, position, duration ->
                if (duration != C.TIME_UNSET) {
                    currentPlayItemList?.getOrNull(index)?.let { currentPlayItem ->
                        if (currentPlayItemIndex != index) {
                            addPlayItem(currentPlayItem)
                            currentPlayItemIndex = index
                        }
                        val episode1 = currentPlayItem.episode
                        val completed = abs(duration - position) < 2000 // 終了まで2秒以内なら再生完了とする
                        val episode2 = episode1.copy(
                            playbackPosition = position,
                            duration = duration,
                            isPlaybackCompleted = completed,
                            lastPlayed = Clock.System.now(),
                        )
                        repo.updateEpisode(episode2)
                    }
                }
            }
        )
    }

    private fun releasePlayer() {
        _audioPlayerFlow.value?.stop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        _audioPlayerFlow.value = null
    }

    /*
        private var lastUri: String? = null
        private fun setMediaItem(player: Player, uri: String, pos: Long) {
            MyLog.d(TAG, "setMediaItem: uri = $uri")
            if (lastUri == uri) return
            lastUri = uri
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem, pos)
            player.prepare()
            player.play()
        }
    */

    override fun onCleared() {
        releasePlayer()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}