package net.kino2718.podcast.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.media3.common.Player
import net.kino2718.podcast.R

@Composable
fun AudioPlayer(
    player: Player,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AudioMetadata(
            player = player,
            modifier = Modifier.fillMaxWidth(),
        )

        AudioControls(
            player = player,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
