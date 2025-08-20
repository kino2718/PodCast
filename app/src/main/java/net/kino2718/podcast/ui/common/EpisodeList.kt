package net.kino2718.podcast.ui.common

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.media3.common.C
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import net.kino2718.podcast.R
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PlaylistItem
import net.kino2718.podcast.ui.utils.format
import net.kino2718.podcast.ui.utils.toHMS

@Composable
fun EpisodeListComposable(
    episodeList: List<Episode>,
    playlistItems: List<PlaylistItem>,
    ascendingOrder: Boolean,
    changeOrder: (Boolean) -> Unit,
    selectEpisode: (Episode, Int) -> Unit,
    addToPlaylist: (Episode, Int) -> Unit,
    download: (Episode, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        var searchFrom by remember { mutableIntStateOf(0) }
        var oldQuery by remember { mutableStateOf("") }

        Tools(
            ascendingOrder = ascendingOrder,
            changeOrder = {
                changeOrder(it)
                searchFrom = 0 // 検索開始位置をリセット
            },
            lastPlayed = {
                val index = episodeList.withIndex()
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
                    val episodes = episodeList
                    val offset = episodes.subList(searchFrom, episodes.size).indexOfFirst {
                        it.title.contains(query, ignoreCase = true)
                    }
                    if (offset < 0) searchFrom = 0 // 見つからなかった
                    else {
                        // 見つかった
                        val index = searchFrom + offset
                        searchFrom = index + 1
                        scope.launch {
                            listState.scrollToItem(index)
                        }
                    }
                }
            }
        )

        EpisodeList(
            episodeList = episodeList,
            playlistItems = playlistItems,
            listState = listState,
            selectEpisode = selectEpisode,
            addToPlaylist = addToPlaylist,
            download = download,
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
    episodeList: List<Episode>,
    playlistItems: List<PlaylistItem>,
    listState: LazyListState,
    selectEpisode: (Episode, Int) -> Unit,
    addToPlaylist: (Episode, Int) -> Unit,
    download: (Episode, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
    ) {
        itemsIndexed(episodeList) { i, episode ->
            val inList = playlistItems.any { it.episodeId == episode.id }
            Episode(
                episode = episode,
                inList = inList,
                selectEpisode = { selectEpisode(it, i) },
                addToPlayList = { addToPlaylist(it, i) },
                download = { download(it, i) },
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
