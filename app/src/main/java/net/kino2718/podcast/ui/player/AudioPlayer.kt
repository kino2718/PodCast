package net.kino2718.podcast.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player

@Composable
fun AudioPlayer(
    player: Player,
    modifier: Modifier = Modifier,
) {
    AudioControls(
        player = player,
        modifier = modifier,
    )
}
