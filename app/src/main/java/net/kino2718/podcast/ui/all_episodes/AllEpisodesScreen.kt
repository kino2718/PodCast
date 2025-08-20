package net.kino2718.podcast.ui.all_episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import net.kino2718.podcast.R
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.common.EpisodeListComposable

@Composable
fun AllEpisodesScreen(
    selectPlayItem: (PlayItem) -> Unit,
    addToPlaylist: (PlayItem) -> Unit,
    download: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AllEpisodesViewModel = viewModel()
) {
    val allPlayItemList by viewModel.allPlayItemsFlow.collectAsState()
    val allEpisodeList = allPlayItemList.map { it.episode }
    val ascendingOrder by viewModel.ascendingOrder.collectAsState()
    val playlistItems by viewModel.playlistItemsFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        EpisodeListComposable(
            episodeList = allEpisodeList,
            playlistItems = playlistItems,
            ascendingOrder = ascendingOrder,
            changeOrder = viewModel::changeOrder,
            selectEpisode = { _, i ->
                allPlayItemList.getOrNull(i)?.let { selectPlayItem(it) }
            },
            addToPlaylist = { _, i ->
                allPlayItemList.getOrNull(i)?.let { addToPlaylist(it) }
            },
            download = { _, i ->
                allPlayItemList.getOrNull(i)?.let { download(it) }
            },
        )
    }
}

@Suppress("unused")
private const val TAG = "AllEpisodesScreen"
