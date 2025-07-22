package net.kino2718.podcast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AppStates(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val channelId: Long? = null,
    val episodeId: Long? = null,
    val inPlaylist: Boolean,
    val speed: Float = 1.0f,
)

