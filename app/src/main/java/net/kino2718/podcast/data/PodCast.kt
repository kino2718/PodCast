package net.kino2718.podcast.data

import androidx.room.Embedded
import androidx.room.Relation

data class PodCast(
    @Embedded
    val channel: PChannel,

    @Relation(
        entity = Episode::class,
        parentColumn = "id",
        entityColumn = "channelId",
    )
    val episodeLists: List<Episode>,
)
