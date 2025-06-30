package net.kino2718.podcast.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    indices = [Index(value = ["guid"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = PChannel::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class Episode(
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
    // 未再生の時はrssから、再生した後はplayerの情報から得る。
    val duration: Long = 0L,// itunes:durationから milli sec
    // 以下は状態
    val downloadFile: String? = null,
    val playbackPosition: Long = 0L, // milli sec
    val isPlaybackCompleted: Boolean = false,
    val lastPlayed: Instant? = null,
) {
    data class Builder(
        var id: Long = 0L,
        var channelId: Long = 0L,
        var guid: String = "",
        var url: String = "",
        var title: String = "",
        var author: String = "",
        var description: String = "",
        var link: String = "",
        var imageUrl: String? = null,
        var pubDate: Instant? = null,
        var duration: Long = 0L,
        var downloadFile: String? = null,
        var playbackPosition: Long = 0L,
        var isPlaybackCompleted: Boolean = false,
        var lastPlayed: Instant? = null,
    ) {
        fun build(): Episode {
            return Episode(
                id = id,
                channelId = channelId,
                guid = guid,
                url = url,
                title = title,
                author = author,
                description = description,
                link = link,
                imageUrl = imageUrl,
                pubDate = pubDate,
                duration = duration,
                downloadFile = downloadFile,
                playbackPosition = playbackPosition,
                isPlaybackCompleted = isPlaybackCompleted,
                lastPlayed = lastPlayed,
            )
        }
    }
}
