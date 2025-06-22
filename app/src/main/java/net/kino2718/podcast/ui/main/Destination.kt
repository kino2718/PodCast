package net.kino2718.podcast.ui.main

import kotlinx.serialization.Serializable
import net.kino2718.podcast.data.Item

@Serializable
object StartDestination

@Serializable
data class PodCastDestination(val feedUrl: String)

@Serializable
data class NowDestination(val item: Item)