package net.kino2718.podcast.ui.start

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.kino2718.podcast.ui.NavItem
import net.kino2718.podcast.ui.home.HomeScreen
import net.kino2718.podcast.ui.search.SearchScreen

@Composable
fun StartScreen(
    navItem: NavItem,
    modifier: Modifier = Modifier,
) {
    when (navItem) {
        NavItem.HOME -> HomeScreen(modifier = modifier)
        NavItem.SEARCH -> SearchScreen(modifier = modifier)
    }
}