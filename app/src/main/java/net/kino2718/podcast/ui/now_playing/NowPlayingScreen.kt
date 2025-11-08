package net.kino2718.podcast.ui.now_playing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import net.kino2718.podcast.R
import net.kino2718.podcast.ui.episode.EpisodeInfo

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = viewModel()
) {
    val playingPodCastInfo by viewModel.playingPodcastInfo.collectAsState()
    playingPodCastInfo?.let {
        EpisodeInfo(
            playItem = it,
            modifier = modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_small))
                .verticalScroll(rememberScrollState()),
        )
    }
}
