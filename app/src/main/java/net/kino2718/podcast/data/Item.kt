package net.kino2718.podcast.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = PChannel::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val channelId: Long = 0L,
    // 以下はrssから取得。ローカルに保存しておく情報。netにアクセスする度に書き換えられる
    val guid: String = "",
    val url: String = "", // audio. enclosure urlから
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val link: String = "", // web site
    val imageUrl: String? = null,
    val pubDate: Instant? = null,
    val duration: Long = 0L,// itunes:durationから
    // 以下は再生状態
    val playbackPosition: Long = 0L,
    val isPlaybackCompleted: Boolean = false,
)
