package net.kino2718.podcast.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.kino2718.podcast.data.Item
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PlayItemId
import net.kino2718.podcast.data.PodCast

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
    fun getChannelByIdFlow(id: Long): Flow<PChannel>

    @Query("select * from PChannel where feedUrl = :feedUrl")
    suspend fun getChannelByFeedUrl(feedUrl: String): PChannel?

    @Upsert
    suspend fun upsertItem(item: Item): Long

    suspend fun safeUpsertItem(item: Item): Long {
        val prevId = item.id
        val id = upsertItem(item)
        return if (0 < id) id else prevId
    }

    @Update
    suspend fun updateItem(item: Item)

    @Query("select * from Item where id = :id")
    fun getItemByIdFlow(id: Long): Flow<Item>

    @Query("select * from Item where id = :id")
    suspend fun getItemById(id: Long): Item?

    @Query("select * from Item where guid = :guid")
    suspend fun getItemByGuid(guid: String): Item?

    @Upsert
    suspend fun upsertPlayItemId(item: PlayItemId): Long

    suspend fun safeUpsertPlayItemId(item: PlayItemId): Long {
        val prevId = item.id
        val id = upsertPlayItemId(item)
        return if (0 < id) id else prevId
    }

    @Query("delete from PlayItemId")
    suspend fun deleteAllPlayItems()

    @Query("select * from PlayItemId")
    fun getAllPlayItemIdsFlow(): Flow<List<PlayItemId>>

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
        // 登録されていたらid, playbackPositionを取得する。そうでなければそのまま。
        val item3 =
            item2?.let { item1.copy(id = it.id, playbackPosition = it.playbackPosition) } ?: item1
        // channelIdをコピーする。
        val item4 = item3.copy(channelId = channelId)
        // 登録されていたらid以外は新しいデータで置き換える。そうでなければそのまま。
        val itemId = safeUpsertItem(item4)
        val item = playItem.item.copy(id = itemId)

        val playItemId = PlayItemId(channelId = channelId, itemId = itemId)
        safeUpsertPlayItemId(playItemId)

        return PlayItem(channel = channel, item = item)
    }

    @Query("select * from PChannel where feedUrl = :feedUrl")
    fun getPodCastFlowByFeedUrl(feedUrl: String): Flow<PodCast?>
}
