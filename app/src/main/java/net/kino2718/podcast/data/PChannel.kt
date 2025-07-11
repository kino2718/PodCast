package net.kino2718.podcast.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(indices = [Index(value = ["feedUrl"], unique = true)])
data class PChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val feedUrl: String = "", // search siteから取得、または直接指定
    // 以下はrssから取得。ローカルに保存しておく情報。netにアクセスする度に書き換えられる
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val link: String = "", // web site
    val imageUrl: String? = null,
    // 以下は状態。
    val subscribed: Boolean = false,
    // 以下は最新のitemから取得する
    val lastUpdate: Instant? = null,
)

data class MutablePChannel(
    var id: Long = 0L,
    var feedUrl: String = "",
    var title: String = "",
    var author: String = "",
    var description: String = "",
    var link: String = "",
    var imageUrl: String? = null,
    var subscribed: Boolean = false,
    var lastUpdate: Instant? = null,
) {
    fun toImmutable(): PChannel {
        return PChannel(
            id = id,
            feedUrl = feedUrl,
            title = title,
            author = author,
            description = description,
            link = link,
            imageUrl = imageUrl,
            subscribed = subscribed,
            lastUpdate = lastUpdate
        )
    }
}