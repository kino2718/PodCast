package net.kino2718.podcast.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.ui.utils.format
import net.kino2718.podcast.ui.utils.toHMS

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = viewModel(),
) {
    val playlistUIStates by viewModel.playlistUIStatesFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        Header(
            title = stringResource(R.string.playlist),
            deleteAll = viewModel::deleteAll,
            modifier = Modifier.fillMaxWidth()
        )

        ItemList(
            playlistUIStates,
            selectItem = {},
            deleteItem = viewModel::deleteItem,
        )
    }
}

@Composable
private fun Header(
    title: String,
    deleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = deleteAll,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium))
            )
        }
    }
}

@Composable
private fun ItemList(
    playlist: List<PlaylistUIState>,
    selectItem: (PlaylistUIState) -> Unit,
    deleteItem: (PlaylistUIState) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(playlist) { i, playlistUIState ->
            Item(
                playlistUIState = playlistUIState,
                selectItem = {},
                deleteItem = deleteItem,
            )
        }
    }
}

@Composable
private fun Item(
    playlistUIState: PlaylistUIState,
    selectItem: (PlaylistUIState) -> Unit,
    deleteItem: (PlaylistUIState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channel = playlistUIState.playItem.channel
    val episode = playlistUIState.playItem.episode
    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small))
                .clickable { selectItem(playlistUIState) },
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // httpだと表示されないため
            val imageUrl = episode.imageUrl?.replaceFirst("http://", "https://")
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.item_image_size))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = channel.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row {
                    episode.pubDate?.let {
                        Text(
                            text = it.format(),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    val playbackPosition = episode.playbackPosition
                    val duration = episode.duration
                    if (duration != C.TIME_UNSET) {
                        val text =
                            if (episode.isPlaybackCompleted) stringResource(R.string.playback_done)
                            else if (0L < playbackPosition) "${playbackPosition.toHMS()}/${duration.toHMS()}"
                            else duration.toHMS()
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
            IconButton(
                onClick = { deleteItem(playlistUIState) },
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small))
                )
            }
        }
    }
}