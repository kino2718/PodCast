package net.kino2718.podcast.ui.start

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.home.HomeScreen
import net.kino2718.podcast.ui.playlist.PlaylistScreen
import net.kino2718.podcast.ui.main.NavItem
import net.kino2718.podcast.ui.search.SearchScreen

@Composable
fun StartScreen(
    navItem: NavItem,
    select: (String) -> Unit,
    selectItem: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (navItem) {
        NavItem.HOME -> HomeScreen(
            select = select,
            selectItem = selectItem,
            modifier = modifier,
        )

        NavItem.SEARCH -> SearchScreen(
            select = select,
            modifier = modifier
        )

        NavItem.PLAYLIST -> PlaylistScreen()
    }
}