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
import androidx.media3.common.C
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.utils.formatToDate
import net.kino2718.podcast.ui.utils.toHMS
import net.kino2718.podcast.ui.utils.toHttps

@Composable
fun HomeScreen(
    select: (String) -> Unit,
    selectItem: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val subscribed by viewModel.subscribedFlow.collectAsState()
    val recentPlays by viewModel.recentPlays.collectAsState()
    val nextEpisodes by viewModel.nextEpisodesFlow.collectAsState()
    val latestEpisodes by viewModel.latestEpisodesFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        Text(
            text = stringResource(R.string.title_my_subscriptions),
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
        ShowPlayItemList(
            title = stringResource(R.string.title_recent_plays),
            playItemList = recentPlays,
            selectItem = selectItem,
        )
        ShowPlayItemList(
            title = stringResource(R.string.title_next_episodes),
            playItemList = nextEpisodes,
            selectItem = selectItem,
        )
        ShowPlayItemList(
            title = stringResource(R.string.title_latest_episodes),
            playItemList = latestEpisodes,
            selectItem = selectItem,
        )
    }
}

@Composable
private fun ShowPlayItemList(
    title: String,
    playItemList: List<PlayItem>,
    selectItem: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playItemList.isNotEmpty()) {
        Text(
            text = title,
            modifier = modifier.padding(bottom = dimensionResource(R.dimen.padding_medium)),
            style = MaterialTheme.typography.titleLarge,
        )
        LazyRow(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(playItemList) { playItem ->
                Box(
                    modifier = Modifier
                        .size(
                            width = dimensionResource(R.dimen.recently_box_width),
                            height = dimensionResource(R.dimen.recently_box_height)
                        )
                        .padding(dimensionResource(R.dimen.padding_extra_small))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.rounded_corner)))
                        .clickable {
                            selectItem(playItem)
                        },
                ) {
                    AsyncImage(
                        model = playItem.episode.imageUrl?.toHttps(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = .4f
                    )
                    Column(modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))) {
                        val channel = playItem.channel
                        val item = playItem.episode
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = channel.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        item.pubDate?.formatToDate(stringResource(R.string.date_template))
                            ?.let { date ->
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        val playbackPosition = item.playbackPosition
                        val duration = item.duration
                        if (duration != C.TIME_UNSET) {
                            val text =
                                if (0L < playbackPosition) "${playbackPosition.toHMS()}/${duration.toHMS()}"
                                else duration.toHMS()
                            Text(
                                text = text,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }

                }
            }
        }
    }
}