package net.kino2718.podcast.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavItem(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    PLAYLIST("Playlist", Icons.AutoMirrored.Filled.PlaylistPlay),
    NOW_PLAYING("Playing", Icons.Default.GraphicEq),
}
