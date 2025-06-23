package net.kino2718.podcast.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import net.kino2718.podcast.utils.MyLog


@Composable
fun rememberSeekForwardButtonState(player: Player): SeekForwardButtonState {
    val seekForwardButtonState = remember(player) { SeekForwardButtonState(player) }
    LaunchedEffect(player) { seekForwardButtonState.observe() }
    return seekForwardButtonState
}

class SeekForwardButtonState(private val player: Player) {
    var isEnabled by mutableStateOf(player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD))
        private set

    fun onClick() {
        MyLog.d("SeekForwardButtonState", "seekForwardIncrement = ${player.seekForwardIncrement}")
        player.seekForward()
    }

    suspend fun observe(): Nothing =
        player.listen { events ->
            if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                isEnabled = isCommandAvailable(Player.COMMAND_SEEK_FORWARD)
            }
        }
}