package net.kino2718.podcast.ui.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import net.kino2718.podcast.R
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PlaylistItem
import net.kino2718.podcast.ui.utils.format
import net.kino2718.podcast.ui.utils.fromHtml
import net.kino2718.podcast.ui.utils.toHMS
import net.kino2718.podcast.ui.utils.toHttps
import net.kino2718.podcast.utils.MyLog

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

            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            var searchFrom by remember { mutableIntStateOf(0) }
            var oldQuery by remember { mutableStateOf("") }

            Tools(
                ascendingOrder = state.ascendingOrder,
                changeOrder = {
                    viewModel.changeOrder(it)
                    searchFrom = 0 // 検索開始位置をリセット
                },
                lastPlayed = {
                    val index = state.podCast.episodeList.withIndex()
                        .maxByOrNull { indexedValue ->
                            indexedValue.value.lastPlayed?.toEpochMilli() ?: 0L
                        }?.index
                    index?.let {
                        searchFrom = 0 // 検索開始位置をリセット
                        scope.launch {
                            listState.scrollToItem(index)
                        }
                    }
                },
                searchEpisodes = { query ->
                    if (query.isNotEmpty()) {
                        if (query != oldQuery) {
                            searchFrom = 0
                            oldQuery = query
                        }
                        val episodes = state.podCast.episodeList
                        val offset = episodes.subList(searchFrom, episodes.size).indexOfFirst {
                            it.title.contains(query, ignoreCase = true)
                        }
                        if (offset < 0) searchFrom = 0 // 見つからなかった
                        else {
                            // 見つかった
                            val index = searchFrom + offset
                            MyLog.d(TAG, "searchEpisodes, query = $query, index = $index")
                            searchFrom = index + 1
                            scope.launch {
                                listState.scrollToItem(index)
                            }
                        }
                    }
                }
            )
            EpisodeList(
                uiState = state,
                playlistItems = playlistItems,
                listState = listState,
                selectEpisode = { episode ->
                    uiState?.let { state ->
                        val playItem = PlayItem(channel = state.podCast.channel, episode = episode)
                        selectPlayItem(playItem)
                    }
                },
                addToPlaylist = { episode ->
                    uiState?.let { state ->
                        val playItem = PlayItem(channel = state.podCast.channel, episode = episode)
                        addToPlaylist(playItem)
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

@Composable
private fun Tools(
    ascendingOrder: Boolean,
    changeOrder: (Boolean) -> Unit,
    lastPlayed: () -> Unit,
    searchEpisodes: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
            horizontalArrangement = spacedBy(dimensionResource(R.dimen.padding_small)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // order
            IconButton(
                onClick = { changeOrder(!ascendingOrder) },
                modifier = Modifier.size(dimensionResource(R.dimen.icon_button_small)),
            ) {
                val image = if (ascendingOrder) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small))
                )
            }
            // last played
            TextButton(
                onClick = lastPlayed,
            ) {
                Text(
                    text = stringResource(R.string.last_played),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            // search
            var query by remember { mutableStateOf("") }
            val keyboardController = LocalSoftwareKeyboardController.current
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.labelMedium,
                singleLine = true,
                label = {
                    Text(
                        text = stringResource(R.string.search_episodes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            searchEpisodes(query)
                            keyboardController?.hide()
                        },
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_button_small))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_small))
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun EpisodeList(
    uiState: PodCastUIState,
    playlistItems: List<PlaylistItem>,
    listState: LazyListState,
    selectEpisode: (Episode) -> Unit,
    addToPlaylist: (Episode) -> Unit,
    download: (Episode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
    ) {
        items(uiState.podCast.episodeList) { episode ->
            val inList = playlistItems.any { it.episodeId == episode.id }
            Episode(
                episode = episode,
                inList = inList,
                selectEpisode = selectEpisode,
                addToPlayList = addToPlaylist,
                download = download,
            )
        }
    }
}

@Composable
private fun Episode(
    episode: Episode,
    inList: Boolean,
    selectEpisode: (Episode) -> Unit,
    addToPlayList: (Episode) -> Unit,
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
                .clickable { selectEpisode(episode) },
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
                    maxLines = 3,
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
            // playlist
            IconButton(
                onClick = { addToPlayList(episode) },
                modifier = Modifier.size(dimensionResource(R.dimen.icon_button_small)),
                enabled = !inList,
            ) {
                val image = if (inList) Icons.AutoMirrored.Filled.PlaylistAddCheck
                else Icons.AutoMirrored.Filled.PlaylistAdd
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
            // download
            val downloaded = episode.downloadFile != null
            IconButton(
                onClick = { download(episode) },
                modifier = Modifier.size(dimensionResource(R.dimen.icon_button_small)),
                enabled = !downloaded,
            ) {
                val image = if (downloaded) Icons.Default.FileDownloadDone
                else Icons.Default.Download
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
        }
    }
}

@Suppress("unused")
private const val TAG = "PodCastScreen"
