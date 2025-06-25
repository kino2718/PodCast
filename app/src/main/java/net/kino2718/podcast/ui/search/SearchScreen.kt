package net.kino2718.podcast.ui.search

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.ui.utils.format

@Suppress("unused")
private const val TAG = "SearchScreen"

@Composable
fun SearchScreen(
    select: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.searchUIStateFlow.collectAsState()
    val subscribed by viewModel.subscribedFlow.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search Podcasts") },
            trailingIcon = {
                IconButton(onClick = {
                    viewModel.searchPodcasts(query)
                    keyboardController?.hide()
                }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.results) { podcastState ->
                Card(
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_small))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.padding_small))
                            .clickable { select(podcastState.feedUrl) },
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = podcastState.artworkUrl100,
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.search_result_image_size))
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = podcastState.collectionName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = podcastState.artistName,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row {
                                Text(
                                    text = podcastState.trackCount.format() + " episodes",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                subscribed.find { it.feedUrl == podcastState.feedUrl }?.let {
                                    Text(
                                        text = stringResource(R.string.subscribed),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
