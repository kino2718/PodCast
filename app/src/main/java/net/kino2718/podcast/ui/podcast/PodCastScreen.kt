package net.kino2718.podcast.ui.podcast

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.utils.format
import net.kino2718.podcast.ui.utils.fromHtml
import net.kino2718.podcast.ui.utils.toHMS
import net.kino2718.podcast.ui.utils.toHttps
import net.kino2718.podcast.utils.MyLog

@Composable
fun PodCastScreen(
    feedUrl: String,
    selectPlayItem: (PlayItem) -> Unit,
    download: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodCastViewModel = viewModel()
) {
    LaunchedEffect(feedUrl) {
        viewModel.load(feedUrl)
    }
    val uiState by viewModel.uiState.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        uiState?.let {
            Channel(
                uiState = it,
                subscribe = viewModel::subscribe,
            )
            ItemList(
                uiState = it,
                selectItem = { episode ->
                    uiState?.let { state ->
                        val playItem = PlayItem(channel = state.podCast.channel, episode = episode)
                        selectPlayItem(playItem)
                    }
                },
                addPlaylist = { episode ->
                    uiState?.let { state ->
                        val playItem = PlayItem(channel = state.podCast.channel, episode = episode)
                        viewModel.addToPlaylist(playItem)
                    }
                },
                download = { episode ->
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
    // subscribedの状態はflowで流れて来ないのでここで保持する
    var subscribed by remember(channel.subscribed) { mutableStateOf(channel.subscribed) }

    MyLog.d(TAG, "imageUrl = ${channel.imageUrl}")
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
            val image = if (!subscribed) Icons.Filled.AddCircle else Icons.Default.Delete
            IconButton(
                onClick = {
                    subscribed = !subscribed
                    subscribe(uiState.podCast.channel, subscribed)
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

@Composable
private fun ItemList(
    uiState: PodCastUIState,
    selectItem: (Episode) -> Unit,
    addPlaylist: (Episode) -> Unit,
    download: (Episode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(uiState.podCast.episodeList) {
            Item(
                episode = it,
                selectItem = selectItem,
                addPlayList = addPlaylist,
                download = download,
            )
        }
    }
}

@Composable
private fun Item(
    episode: Episode,
    selectItem: (Episode) -> Unit,
    addPlayList: (Episode) -> Unit,
    download: (Episode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small))
                .clickable { selectItem(episode) },
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
                    maxLines = 2,
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
                onClick = { addPlayList(episode) },
                modifier = Modifier.size(dimensionResource(R.dimen.icon_button_small))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
            IconButton(
                onClick = { download(episode) },
                modifier = Modifier.size(dimensionResource(R.dimen.icon_button_small))
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
        }
    }
}

@Suppress("unused")
private const val TAG = "PodCastScreen"
