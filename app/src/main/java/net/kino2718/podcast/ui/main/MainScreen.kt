package net.kino2718.podcast.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import net.kino2718.podcast.R
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.player.AudioPlayer
import net.kino2718.podcast.ui.player.rememberPlaybackPositionState
import net.kino2718.podcast.ui.podcast.PodCastScreen
import net.kino2718.podcast.ui.start.StartScreen
import net.kino2718.podcast.ui.utils.InteractiveLinearProgressIndicator
import net.kino2718.podcast.ui.utils.toHMS

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
    var current by rememberSaveable { mutableStateOf(NavItem.HOME) }
    val lastPlayedItem by viewModel.lastPlayedItemFlow.collectAsState()
    val playItem by viewModel.playItemFlow.collectAsState()
    val player by viewModel.audioPlayerFlow.collectAsState()
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                val item = playItem
                val pl = player
                val last = lastPlayedItem
                if (item != null && pl != null) {
                    Control(
                        player = pl,
                        playItem = item,
                    )
                } else if (last != null) {
                    LastPlayedItem(
                        lastPlayedItem = last,
                        selectItem = viewModel::setPlayItem
                    )
                }
                NavigationBar {
                    NavItem.entries.forEach { item ->
                        NavigationBarItem(
                            selected = current == item,
                            onClick = {
                                // Navigationで移動している時は最初の画面に戻る
                                while (navController.previousBackStackEntry != null) navController.navigateUp()
                                current = item
                            },
                            icon = {
                                androidx.compose.material3.Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = StartDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<StartDestination> {
                StartScreen(
                    navItem = current,
                    select = {
                        val podCastDestination = PodCastDestination(feedUrl = it)
                        navController.navigate(podCastDestination)
                    },
                    selectItem = viewModel::setPlayItem,
                )
            }
            composable<PodCastDestination> { navBackStackEntry ->
                val podCastDestination = navBackStackEntry.toRoute<PodCastDestination>()
                PodCastScreen(
                    podCastDestination.feedUrl,
                    selectPlayItem = {
                        viewModel.setPlayItem(it)
                    }
                )
            }
        }
    }
}

@Composable
fun LastPlayedItem(
    lastPlayedItem: PlayItem,
    selectItem: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channel = lastPlayedItem.channel
    val item = lastPlayedItem.episode

    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small))
                .clickable { selectItem(lastPlayedItem) },
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
                val playbackPosition = item.playbackPosition.toHMS()
                val duration = item.duration.toHMS()
                val pos = "$playbackPosition/$duration"
                Text(
                    text = pos,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
fun Control(
    player: Player,
    playItem: PlayItem,
    modifier: Modifier = Modifier,
) {
    val channel = playItem.channel
    val item = playItem.episode

    val positionState = rememberPlaybackPositionState(player)
    val playbackPosition = positionState.playbackPosition.position
    val duration = positionState.playbackPosition.duration

    Card(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))
    ) {
        var offset by remember { mutableFloatStateOf(0f) }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small))
                .pointerInput(Unit) {
                    val width = size.width
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            seekTo(player, offset / width)
                            offset = 0f
                        },
                        onDragCancel = {
                            seekTo(player, offset / width)
                            offset = 0f
                        }
                    ) { _, dragAmount ->
                        offset += dragAmount
                    }
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
                if (duration != C.TIME_UNSET) {
                    val pos = "${playbackPosition.toHMS()}/${duration.toHMS()}"
                    Text(
                        text = pos,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        val progress = if (duration != 0L)
            (playbackPosition.toFloat() / duration.toFloat()).coerceAtLeast(0f).coerceAtMost(1f)
        else 0f

        InteractiveLinearProgressIndicator(
            progress = progress,
            onTappedFraction = {
                // tap位置にseekする。
                val newPos =
                    (duration * it).toLong().coerceAtLeast(0L).coerceAtMost(duration - 100L)
                player.seekTo(newPos)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_small)),
        )

        AudioPlayer(
            player = player,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(R.dimen.padding_large),
                    bottom = dimensionResource(R.dimen.padding_small)
                )
                .pointerInput(Unit) {},
        )
    }
}

private fun seekTo(player: Player, amount: Float) {
    // 端から端までswipeで2分とする
    val seekAmount = (amount * 2 * 60 * 1000).toLong()
    val newPosition =
        (player.contentPosition + seekAmount)
            .coerceAtLeast(0L)
            .coerceAtMost(player.duration - 100L)
    player.seekTo(newPosition)
}
