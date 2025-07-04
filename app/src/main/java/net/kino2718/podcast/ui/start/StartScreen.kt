package net.kino2718.podcast.ui.start

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.ui.home.HomeScreen
import net.kino2718.podcast.ui.main.NavItem
import net.kino2718.podcast.ui.now_playing.NowPlayingScreen
import net.kino2718.podcast.ui.playlist.PlaylistScreen
import net.kino2718.podcast.ui.search.SearchScreen

@Composable
fun StartScreen(
    navItem: NavItem,
    selectFeedUrl: (String) -> Unit,
    selectItem: (PlayItem) -> Unit,
    selectItems: (List<PlayItem>, Int) -> Unit,
    addToPlaylist: (PlayItem) -> Unit,
    download: (PlayItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (navItem) {
        NavItem.HOME -> HomeScreen(
            select = selectFeedUrl,
            selectItem = selectItem,
            addToPlaylist = addToPlaylist,
            download = download,
            modifier = modifier,
        )

        NavItem.SEARCH -> SearchScreen(
            selectFeedUrl = selectFeedUrl,
            modifier = modifier
        )

        NavItem.PLAYLIST -> PlaylistScreen(
            selectItems = selectItems,
            download = download,
        )

        NavItem.NOW_PLAYING -> NowPlayingScreen()
    }
}