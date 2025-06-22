package net.kino2718.podcast.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import net.kino2718.podcast.ui.podcast.PodCastScreen
import net.kino2718.podcast.ui.start.StartScreen

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    var current by rememberSaveable { mutableStateOf(NavItem.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavItem.entries.forEach { item ->
                    NavigationBarItem(
                        selected = current == item,
                        onClick = { current = item },
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
    ) { innerPadding ->
        val navController = rememberNavController()
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
                )
            }
            composable<PodCastDestination> { navBackStackEntry ->
                val podCastDestination = navBackStackEntry.toRoute<PodCastDestination>()
                PodCastScreen(podCastDestination.feedUrl)
            }
        }
    }
}
