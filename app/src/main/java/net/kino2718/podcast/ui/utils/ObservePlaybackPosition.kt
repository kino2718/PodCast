package net.kino2718.podcast.ui.utils

import androidx.media3.common.Player
import androidx.media3.common.listen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ObservePlaybackPosition {
    private var observationJob: Job? = null

    suspend fun observe(
        player: Player,
        scope: CoroutineScope,
        onChanged: suspend (index: Int, position: Long, duration: Long) -> Unit,
    ): Nothing {
        pollingWhenPlaying(player, scope, onChanged)

        player.listen { events ->
            scope.launch {
                // play, pause時
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    pollingWhenPlaying(player, scope, onChanged)
                }

                // seek時
                if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                    readPosition(player, onChanged)
                }
            }
        }
    }

    private suspend fun pollingWhenPlaying(
        player: Player,
        scope: CoroutineScope,
        onChanged: suspend (index: Int, position: Long, duration: Long) -> Unit,
    ) {
        if (player.isPlaying) {
            observationJob?.let { if (it.isActive) return } // 重複回避
            observationJob = scope.launch {
                startObservationOfPlaybackPosition(player, onChanged)
            }
        } else {
            stopObservationOfPlaybackPosition(player, onChanged)
        }
    }

    private suspend fun CoroutineScope.startObservationOfPlaybackPosition(
        player: Player,
        onChanged: suspend (index: Int, position: Long, duration: Long) -> Unit,
    ) {
        while (isActive) {
            readPosition(player, onChanged)
            // delayの値を計算
            val diff = player.currentPosition % 1000L
            val d = if (diff <= 500L) 1000L - diff
            else 2000L - diff
            delay(d)
        }
    }

    private suspend fun stopObservationOfPlaybackPosition(
        player: Player,
        onChanged: suspend (index: Int, position: Long, duration: Long) -> Unit
    ) {
        readPosition(player, onChanged)

        observationJob?.cancel()
        observationJob = null
    }

    private suspend fun readPosition(
        player: Player,
        onRead: suspend (index: Int, position: Long, duration: Long) -> Unit,
    ) {
        val position = player.currentPosition
        val duration = player.duration
        val index = player.currentMediaItemIndex
        onRead(index, position, duration)
    }
}
