package net.kino2718.podcast.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.utils.toHttps

@Composable
fun HomeScreen(
    select: (String) -> Unit,
    selectItem: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val subscribed by viewModel.subscribedFlow.collectAsState()
    val recentlyListened by viewModel.recentlyListenedFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        Text(
            text = stringResource(R.string.title_subscribed),
            style = MaterialTheme.typography.titleLarge
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.height(dimensionResource(R.dimen.subscribed_grid_size) * 2)
        ) {
            items(subscribed) {
                AsyncImage(
                    model = it.imageUrl?.toHttps(), // httpだと表示されないため
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.padding_extra_small))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.rounded_corner)))
                        .clickable {
                            select(it.feedUrl)
                        }
                )
            }
        }
        Text(
            text = stringResource(R.string.title_recently_listened),
            style = MaterialTheme.typography.titleLarge,
        )
        LazyRow(verticalAlignment = Alignment.CenterVertically) {
            items(recentlyListened) {
                Box(
                    modifier = Modifier
                        .size(
                            width = dimensionResource(R.dimen.recently_box_width),
                            height = dimensionResource(R.dimen.recently_box_height)
                        )
                        .padding(dimensionResource(R.dimen.padding_extra_small))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.rounded_corner)))
                        .clickable {
                            viewModel.addLastPlayedItem(it.channel, it.episode)
                            selectItem(it)
                        },
                ) {
                    AsyncImage(
                        model = it.episode.imageUrl?.toHttps(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = .4f
                    )
                    Column(modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))) {
                        val channel = it.channel
                        val item = it.episode
                        Text(
                            text = item.title,
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
                        /*
                                                val playbackPosition = item.playbackPosition.toHMS()
                                                val duration = item.duration.toHMS()
                                                val pos = "$playbackPosition/$duration"
                                                Text(
                                                    text = pos,
                                                    style = MaterialTheme.typography.titleMedium,
                                                )
                        */
                    }

                }
            }
        }
    }
}
