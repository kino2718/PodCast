package net.kino2718.podcast.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import net.kino2718.podcast.ui.now.NowScreen
import net.kino2718.podcast.ui.podcast.PodCastScreen
import net.kino2718.podcast.ui.start.StartScreen

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = StartDestination,
        modifier = modifier,
    ) {
        composable<StartDestination> {
            StartScreen(
                select = {
                    val podCastDestination = PodCastDestination(feedUrl = it)
                    navController.navigate(podCastDestination)
                }
            )
        }
        composable<PodCastDestination> { navBackStackEntry ->
            val podCastDestination = navBackStackEntry.toRoute<PodCastDestination>()
            PodCastScreen(
                podCastDestination.feedUrl,
                selectItem = {
                    val nowDestination = NowDestination(item = it)
                    navController.navigate(nowDestination)
                }
            )
        }
        composable<NowDestination> { navBackStackEntry ->
            val nowDestination = navBackStackEntry.toRoute<NowDestination>()
            NowScreen(
                item = nowDestination.item,
            )
        }
    }
}