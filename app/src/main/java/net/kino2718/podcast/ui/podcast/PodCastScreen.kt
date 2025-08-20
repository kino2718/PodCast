package net.kino2718.podcast.ui.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.common.EpisodeListComposable
import net.kino2718.podcast.ui.utils.format
import net.kino2718.podcast.ui.utils.fromHtml
import net.kino2718.podcast.ui.utils.toHttps

@Composable
fun PodCastScreen(
    feedUrl: String,
    selectPlayItem: (PlayItem) -> Unit,
    addToPlaylist: (PlayItem) -> Unit,
    download: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodCastViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlistItems by viewModel.playlistItemsFlow.collectAsState()
    LaunchedEffect(feedUrl) {
        viewModel.load(feedUrl)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        uiState?.let { state ->
            Channel(
                uiState = state,
                subscribe = viewModel::subscribe,
            )

            EpisodeListComposable(
                episodeList = state.podCast.episodeList,
                playlistItems = playlistItems,
                ascendingOrder = state.ascendingOrder,
                changeOrder = viewModel::changeOrder,
                selectEpisode = { episode, _ ->
                    uiState?.let { state ->
                        val playItem = PlayItem(channel = state.podCast.channel, episode = episode)
                        selectPlayItem(playItem)
                    }
                },
                addToPlaylist = { episode, _ ->
                    uiState?.let { state ->
                        val playItem = PlayItem(channel = state.podCast.channel, episode = episode)
                        addToPlaylist(playItem)
                    }
                },
                download = { episode, _ ->
                    uiState?.let { state ->
                        val playItem = PlayItem(channel = state.podCast.channel, episode = episode)
                        download(playItem)
                    }
                },
            )
        }
    }
}

@Composable
private fun Channel(
    uiState: PodCastUIState,
    subscribe: (PChannel, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channel = uiState.podCast.channel

    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_small))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imageUrl = channel.imageUrl?.toHttps() // httpだと表示されないため
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.channel_image_size)),
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = channel.author,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = uiState.podCast.episodeList.size.format() + " episodes",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            val image =
                if (!channel.subscribed) Icons.Default.AddCircle else Icons.Default.RemoveCircle
            IconButton(
                onClick = {
                    subscribe(uiState.podCast.channel, !channel.subscribed)
                },
            ) {
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }

        Text(
            text = channel.description.fromHtml(),
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Suppress("unused")
private const val TAG = "PodCastScreen"
