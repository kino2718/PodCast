package net.kino2718.podcast.ui.main

import kotlinx.serialization.Serializable

@Serializable
object StartDestination

@Serializable
data class PodCastDestination(val feedUrl: String)

@Serializable
object AllEpisodesDestination

@Serializable
data class EpisodeInfoDestination(val episodeId: Long)
