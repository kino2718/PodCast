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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import net.kino2718.podcast.data.Item
import net.kino2718.podcast.ui.format
import net.kino2718.podcast.ui.toHMS
import net.kino2718.podcast.utils.MyLog

@Composable
fun PodCastScreen(
    feedUrl: String,
    selectItem: (Item) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodCastViewModel = viewModel()
) {
    LaunchedEffect(feedUrl) {
        viewModel.load(feedUrl)
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
        ) {
            uiState?.let {
                Channel(uiState = it)
                ItemList(
                    uiState = it,
                    selectItem = selectItem,
                    )
            }
        }
    }
}

@Composable
private fun Channel(
    uiState: PodCastUIState,
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
        }

        Text(
            text = channel.description,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
            maxLines = 10,
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
        items(items) { item ->
            Item(
                item = item,
                selectItem = selectItem,
            )
        }
    }
}

@Composable
private fun Item(
    item: Item,
    selectItem: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small))
                .clickable {
                    selectItem(item)
                },
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
                    Text(
                        text = item.duration.toHMS(),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

private const val TAG = "PodCastScreen"
