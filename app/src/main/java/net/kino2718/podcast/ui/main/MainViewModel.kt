package net.kino2718.podcast.ui.main

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.service.PlaybackService
import net.kino2718.podcast.utils.MyLog

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext = app.applicationContext
    private val repo = Repository(appContext)
    val playItemFlow =
        repo.getPlayListFlow()
            .mapNotNull { list ->
                // ToDo: 今のところplay listはアイテム1つのみなので先頭のアイテムを取り出す
                if (list.isNotEmpty()) list[0] else null
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var _audioPlayerFlow = MutableStateFlow<Player?>(null)
    val audioPlayerFlow = _audioPlayerFlow.asStateFlow()

    init {
        viewModelScope.launch {
            initializePlayer()
        }
        viewModelScope.launch {
            combine(audioPlayerFlow, playItemFlow) { player, playItem ->
                MyLog.d(TAG, "player: $player, playItem: $playItem")
                if (player != null && playItem != null) setMediaItem(player, playItem.item.url)
                Unit
            }.collect {}
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
    }

    private fun releasePlayer() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        _audioPlayerFlow.value = null
    }

    private fun setMediaItem(player: Player, uri: String) {
        MyLog.d(TAG, "setMediaItem: uri = $uri")
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem, 0L)
        player.prepare()
    }

    override fun onCleared() {
        releasePlayer()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}