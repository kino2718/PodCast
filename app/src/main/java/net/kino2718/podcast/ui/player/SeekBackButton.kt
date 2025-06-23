package net.kino2718.podcast.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import net.kino2718.podcast.R

@Composable
fun SeekBackButton(
    player: Player,
    modifier: Modifier = Modifier
) {
    val state = rememberSeekBackButtonState(player)

    IconButton(
        onClick = state::onClick,
        modifier = modifier,
        enabled = state.isEnabled,
    ) {
        Icon(
            imageVector = Icons.Default.Replay10,
            contentDescription = stringResource(R.string.seek_back_button),
            modifier = modifier
        )
    }
}