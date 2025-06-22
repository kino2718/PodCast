package net.kino2718.podcast.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.kino2718.podcast.data.Item
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PlayListTableItem

@Dao
interface PodCastDao {
    @Upsert
    suspend fun upsertChannel(channel: PChannel): Long

    suspend fun safeUpsertChannel(channel: PChannel): Long {
        val prevId = channel.id
        val id = upsertChannel(channel)
        return if (0 < id) id else prevId
    }

    @Query("select * from PChannel where id = :id")
    suspend fun getChannelById(id: Long): PChannel?

    @Query("select * from PChannel where feedUrl = :feedUrl")
    suspend fun getChannelByFeedUrl(feedUrl: String): PChannel?

    @Upsert
    suspend fun upsertItem(item: Item): Long

    suspend fun safeUpsertItem(item: Item): Long {
        val prevId = item.id
        val id = upsertItem(item)
        return if (0 < id) id else prevId
    }

    @Query("select * from Item where id = :id")
    suspend fun getItemById(id: Long): Item?

    @Query("select * from Item where guid = :guid")
    suspend fun getItemByGuid(guid: String): Item?

    @Upsert
    suspend fun upsertPlayListTableItem(item: PlayListTableItem): Long

    suspend fun safeUpsertPlayListTableItem(item: PlayListTableItem): Long {
        val prevId = item.id
        val id = upsertPlayListTableItem(item)
        return if (0 < id) id else prevId
    }

    @Query("delete from PlayListTableItem")
    suspend fun deleteAllPlayItems()

    @Query("select * from PlayListTableItem")
    fun getAllPlayListTableItemFlow(): Flow<List<PlayListTableItem>>

    @Transaction
    suspend fun addPlayItem(playItem: PlayItem): PlayItem {
        val channel1 = playItem.channel
        // feedUrlで既に登録されているかを調べる。
        val channel2 = getChannelByFeedUrl(channel1.feedUrl)
        // 登録されていたらidだけ取得する。そうでなければそのまま。
        val channel3 = channel2?.let { channel1.copy(id = it.id) } ?: channel1
        // 登録されていたらid以外は新しいデータで置き換える。そうでなければそのまま。
        val channelId = safeUpsertChannel(channel3)
        val channel = channel3.copy(id = channelId)

        val item1 = playItem.item
        // guidで既に登録されているかを調べる。
        val item2 = getItemByGuid(item1.guid)
        // 登録されていたらidだけ取得する。そうでなければそのまま。
        val item3 = item2?.let { item1.copy(id = it.id) } ?: item1
        // channelIdをコピーする。
        val item4 = item3.copy(channelId = channelId)
        // 登録されていたらid以外は新しいデータで置き換える。そうでなければそのまま。
        val itemId = safeUpsertItem(item4)
        val item = playItem.item.copy(id = itemId)

        val playListTableItem = PlayListTableItem(channelId = channelId, itemId = itemId)
        safeUpsertPlayListTableItem(playListTableItem)

        return PlayItem(channel = channel, item = item)
    }

    fun getPlayListFlow(): Flow<List<PlayItem>> {
        return getAllPlayListTableItemFlow().map { list ->
            list.mapNotNull { ti ->
                val channel = getChannelById(ti.channelId)
                val item = getItemById(ti.itemId)
                channel?.let { c ->
                    item?.let { i ->
                        PlayItem(channel = c, item = i, lastPlay = ti.lastPlay)
                    }
                }
            }
        }
    }
}
