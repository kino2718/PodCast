package net.kino2718.podcast.ui.main

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
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
        .mapNotNull { list ->
            if (list.isNotEmpty()) list[0] else null
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val lastPlayedItemFlow = lastPlayedItemIdFlow
        .flatMapLatest {
            // id指定でchannel flowとitem flowを取得しcombineしてFlow<PlayItem>を作成しflatMapする。
            val channelFlow = repo.getChannelByIdFlow(it.channelId)
            val itemFlow = repo.getItemByIdFlow(it.itemId)
            combine(channelFlow, itemFlow) { channel, item ->
                PlayItem(channel = channel, item = item, lastPlay = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _playItemFlow = MutableStateFlow<PlayItem?>(null)
    val playItemFlow = _playItemFlow.asStateFlow()

    fun setPlayItem(playItem: PlayItem) {
        _playItemFlow.value = playItem
        setPlayer(playItem)
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var _audioPlayerFlow = MutableStateFlow<Player?>(null)
    val audioPlayerFlow = _audioPlayerFlow.asStateFlow()

    init {
        viewModelScope.launch {
            initializePlayer()
        }
    }

    private fun setPlayer(playItem: PlayItem) {
        viewModelScope.launch {
            val item = playItem.item
            _audioPlayerFlow.value?.let { player ->
                // posの値はdurationよりある程度小さくしないとExoPlayerから返ってくるdurationの値が異常に小さくなる
                val pos = item.playbackPosition.coerceIn(0L, max(0L, item.duration - 100L))
                setMediaItem(player, item.url, pos)
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
            onChanged = { position, duration ->
                if (duration != C.TIME_UNSET) {
                    val item1 = _playItemFlow.value?.item
                    item1?.let {
                        val completed = abs(duration - position) < 2000 // 終了まで2秒以内なら再生完了とする
                        MyLog.d(
                            TAG,
                            "ObservePlaybackPosition.observe: position = $position, duration = $duration, completed = $completed"
                        )
                        val item2 = it.copy(
                            playbackPosition = position,
                            duration = duration,
                            isPlaybackCompleted = completed
                        )
                        repo.updateItem(item2)
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

    override fun onCleared() {
        releasePlayer()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}