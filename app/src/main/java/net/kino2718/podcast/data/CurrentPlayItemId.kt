package net.kino2718.podcast.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = PChannel::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Episode::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class CurrentPlayItemId(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val channelId: Long = 0L,
    val episodeId: Long = 0L,
    val inPlaylist: Boolean,
)