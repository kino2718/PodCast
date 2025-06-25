package net.kino2718.podcast.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import net.kino2718.podcast.ui.utils.ObservePlaybackPosition

@Composable
fun rememberPlaybackPositionState(player: Player): PlaybackPositionState {
    val scope = rememberCoroutineScope()
    val playbackPositionState = remember(player) { PlaybackPositionState(player) }
    LaunchedEffect(player) { playbackPositionState.observe(scope) }
    return playbackPositionState
}

data class PlaybackPosition(val position: Long = 0L, val duration: Long = C.TIME_UNSET)

class PlaybackPositionState(private val player: Player) {
    var playbackPosition by mutableStateOf(PlaybackPosition())
        private set

    suspend fun observe(scope: CoroutineScope): Nothing {
        ObservePlaybackPosition().observe(
            player = player,
            scope = scope,
            onChanged = { position, duration ->
                playbackPosition = PlaybackPosition(position, duration)
            },
        )
    }
}
