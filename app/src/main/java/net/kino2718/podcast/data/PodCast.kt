package net.kino2718.podcast.data

import androidx.room.Embedded
import androidx.room.Relation

data class PodCast(
    @Embedded
    val channel: PChannel,

    @Relation(
        entity = Item::class,
        parentColumn = "id",
        entityColumn = "channelId",
    )
    val itemList: List<Item>,
)
