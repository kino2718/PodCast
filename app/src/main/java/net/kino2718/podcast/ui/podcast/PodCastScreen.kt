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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import net.kino2718.podcast.R
import net.kino2718.podcast.data.Item
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.utils.format
import net.kino2718.podcast.ui.utils.toHMS
import net.kino2718.podcast.utils.MyLog

@Composable
fun PodCastScreen(
    feedUrl: String,
    selectPlayItem: (PlayItem) -> Unit,
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
        val scope = rememberCoroutineScope()
        uiState?.let {
            Channel(
                uiState = it,
                subscribe = viewModel::subscribe,
            )
            ItemList(
                uiState = it,
                selectItem = { item ->
                    uiState?.let { state ->
                        scope.launch {
                            // 選択したitemを登録する。
                            val playItem = viewModel.addLastPlayedItem(state.podCast.channel, item)
                            selectPlayItem(playItem)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun Channel(
    uiState: PodCastUIState,
    subscribe: (PChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channel = uiState.podCast.channel

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
            // httpだと表示されないため
            val imageUrl = channel.imageUrl?.replaceFirst("http://", "https://")
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
                    text = uiState.podCast.itemList.size.format() + " episodes",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (!uiState.podCast.channel.subscribed) {
                IconButton(
                    onClick = { subscribe(uiState.podCast.channel) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                }
            }
        }

        Text(
            text = channel.description,
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
    selectItem: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        val items = uiState.podCast.itemList.map {
            if (it.imageUrl == null) it.copy(imageUrl = uiState.podCast.channel.imageUrl)
            else it
        }

        items(items) {
            Item(
                item = it,
                selectItem = { selectItem(it) }
            )
        }
    }
}

@Composable
private fun Item(
    item: Item,
    selectItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small))
                .clickable { selectItem() },
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // httpだと表示されないため
            val imageUrl = item.imageUrl?.replaceFirst("http://", "https://")
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.item_image_size))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row {
                    item.pubDate?.let {
                        Text(
                            text = it.format(),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    val t = if (item.playbackPosition == 0L) item.duration.toHMS()
                    else if (item.isPlaybackCompleted) stringResource(R.string.playback_done)
                    else "${item.playbackPosition.toHMS()}/${item.duration.toHMS()}"
                    Text(
                        text = t,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

private const val TAG = "PodCastScreen"
