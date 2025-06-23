package net.kino2718.podcast.ui.player

import androidx.annotation.OptIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import net.kino2718.podcast.R

@OptIn(UnstableApi::class)
@Composable
fun PlayPauseButton(
    player: Player,
    modifier: Modifier = Modifier,
) {
    val state = rememberPlayPauseButtonState(player)
    val icon = if (state.showPlay) Icons.Default.PlayArrow else Icons.Default.Pause
    val contentDescription =
        if (state.showPlay) stringResource(R.string.play_pause_button_play)
        else stringResource(R.string.play_pause_button_pause)

    IconButton(
        onClick = state::onClick,
        modifier = modifier,
        enabled = state.isEnabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}