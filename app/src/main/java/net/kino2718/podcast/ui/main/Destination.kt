package net.kino2718.podcast.ui.main

import kotlinx.serialization.Serializable
import net.kino2718.podcast.data.Item
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
object StartDestination

@Serializable
data class PodCastDestination(val feedUrl: String)

@Serializable
data class NowDestination(val itemJson: String) {
    fun toItem(): Item = Json.decodeFromString(itemJson)
}
