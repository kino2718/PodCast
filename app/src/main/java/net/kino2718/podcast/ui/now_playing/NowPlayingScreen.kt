package net.kino2718.podcast.ui.now_playing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.ui.utils.fromHtml
import net.kino2718.podcast.ui.utils.toHttps

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = viewModel()
) {
    val playingPodCastInfo by viewModel.playingPodcastInfo.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_small))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
    ) {
        playingPodCastInfo?.let {
            Channel(channel = it.channel)
            Episode(it.episode)
        }
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
                }
            }
            Text(
                text = episode.description.fromHtml(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}