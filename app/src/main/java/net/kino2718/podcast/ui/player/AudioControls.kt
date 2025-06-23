package net.kino2718.podcast.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.media3.common.Player
import net.kino2718.podcast.R

@Composable
internal fun AudioControls(
    player: Player,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buttonModifier = Modifier
            .size(dimensionResource(R.dimen.control_icon_size))
            .background(
                color = Color.Gray.copy(alpha = 0.1f),
                shape = CircleShape
            )

        SeekBackButton(player, buttonModifier)
        PlayPauseButton(player, buttonModifier)
        SeekForwardButton(player, buttonModifier)
    }
}
