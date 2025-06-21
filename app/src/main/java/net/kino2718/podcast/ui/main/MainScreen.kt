package net.kino2718.podcast.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            StartScreen()
        }
    }
}