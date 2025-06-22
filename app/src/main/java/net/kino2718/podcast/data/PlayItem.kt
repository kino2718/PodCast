package net.kino2718.podcast.data

data class PlayItem(
    val channel: PChannel,
    val item: Item,
    val lastPlay: Boolean = false,
)