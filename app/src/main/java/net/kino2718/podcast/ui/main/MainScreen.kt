package net.kino2718.podcast.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import net.kino2718.podcast.R
import net.kino2718.podcast.ui.player.AudioPlayer
import net.kino2718.podcast.ui.podcast.PodCastScreen
import net.kino2718.podcast.ui.start.StartScreen

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
    var current by rememberSaveable { mutableStateOf(NavItem.HOME) }
    val player by viewModel.audioPlayerFlow.collectAsState()
    val showControl by viewModel.showControl.collectAsState()
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                player?.let {
                    if (showControl) Control(player = it)
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
                                Icon(
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
                    selectFeedUrl = {
                        val podCastDestination = PodCastDestination(feedUrl = it)
                        navController.navigate(podCastDestination)
                    },
                    selectItem = viewModel::setPlayItem,
                    selectItems = viewModel::setPlayItems,
                    download = viewModel::download,
                )
            }
            composable<PodCastDestination> { navBackStackEntry ->
                val podCastDestination = navBackStackEntry.toRoute<PodCastDestination>()
                PodCastScreen(
                    podCastDestination.feedUrl,
                    selectPlayItem = viewModel::setPlayItem,
                    download = viewModel::download,
                )
            }
        }
    }
}

@Composable
fun Control(
    player: Player,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small))) {
        AudioPlayer(
            player = player,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_small))
        )
    }
}
