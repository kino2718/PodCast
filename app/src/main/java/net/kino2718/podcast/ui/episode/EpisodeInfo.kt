package net.kino2718.podcast.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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

@Composable
fun EpisodeInfoScreen(
    episodeId: Long,
    modifier: Modifier = Modifier,
    viewModel: EpisodeInfoViewModel = viewModel(),
) {
    val playItemState by viewModel.playItem.collectAsState()

    LaunchedEffect(episodeId) {
        viewModel.requestPlayItem(episodeId)
    }

    playItemState?.let {
        EpisodeInfo(
            playItem = it,
            modifier = modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_small))
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
fun EpisodeInfo(
    playItem: PlayItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
    ) {
        Channel(channel = playItem.channel)
        Episode(playItem.episode)
    }
}


@Composable
private fun Channel(
    channel: PChannel,
    modifier: Modifier = Modifier,
) {
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
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = channel.author,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = channel.link,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        Text(
            text = channel.description.fromHtml(),
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
            style = MaterialTheme.typography.bodyMedium,
        )
    }

}

@Composable
private fun Episode(
    episode: Episode,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
        ) {
            Row(
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
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = episode.author,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = episode.link,
                        style = MaterialTheme.typography.titleSmall,
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
            }
            Text(
                text = episode.description.fromHtml(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
