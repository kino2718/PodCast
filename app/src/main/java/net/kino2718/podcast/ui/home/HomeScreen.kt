package net.kino2718.podcast.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.ui.utils.toHttps

@Composable
fun HomeScreen(
    select: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val subscribed by viewModel.subscribedFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        Text(
            text = stringResource(R.string.title_subscribed),
            style = MaterialTheme.typography.titleLarge
        )
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val columnWidth = maxWidth / 3
            LazyVerticalGrid(
                columns = GridCells.FixedSize(size = columnWidth),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.Start,
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
        }
    }
}
