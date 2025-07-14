package net.kino2718.podcast.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen

@Composable
fun rememberSeekBackButtonState(player: Player): SeekBackButtonState {
    val seekBackButtonState = remember(player) { SeekBackButtonState(player) }
    LaunchedEffect(player) { seekBackButtonState.observe() }
    return seekBackButtonState
}

class SeekBackButtonState(private val player: Player) {
    var isEnabled by mutableStateOf(player.isCommandAvailable(Player.COMMAND_SEEK_BACK))
        private set

    fun onClick() {
        player.seekBack()
    }

    suspend fun observe(): Nothing =
        player.listen { events ->
            if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                isEnabled = isCommandAvailable(Player.COMMAND_SEEK_BACK)
            }
        }
}