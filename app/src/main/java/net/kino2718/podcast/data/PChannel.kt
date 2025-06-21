package net.kino2718.podcast.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(indices = [Index(value = ["feedUrl"], unique = true)])
data class PChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val feedUrl: String, // search siteから取得、または直接指定
    // 以下はrssから取得。ローカルに保存しておく情報。netにアクセスする度に書き換えられる
    val title: String,
    val author: String,
    val description: String,
    val link: String, // web site
    val category: String,
    val imageFile: String, // 画像をダウンロードして保存しておくファイル名
    // 以下は最新のitemから取得する
    val lastUpdate: Instant? = null,
)