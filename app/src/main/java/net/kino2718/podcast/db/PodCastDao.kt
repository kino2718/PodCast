package net.kino2718.podcast.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.kino2718.podcast.data.Episode
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
    suspend fun getChannelById(id: Long): PChannel?

    @Query("select * from PChannel where feedUrl = :feedUrl")
    suspend fun getChannelByFeedUrl(feedUrl: String): PChannel?

    @Query("select * from PChannel where subscribed = true")
    fun subscribedChannelFlow(): Flow<List<PChannel>>

    @Upsert
    suspend fun upsertEpisode(episode: Episode): Long

    suspend fun safeUpsertEpisode(episode: Episode): Long {
        val prevId = episode.id
        val id = upsertEpisode(episode)
        return if (0 < id) id else prevId
    }

    @Update
    suspend fun updateEpisode(episode: Episode)

    @Query("select * from Episode where id = :id")
    suspend fun getEpisodeById(id: Long): Episode?

    @Query("select * from Episode where guid = :guid")
    suspend fun getEpisodeByGuid(guid: String): Episode?

    @Query("select * from Episode where isPlaybackCompleted = false order by lastPlayed desc limit :limits")
    fun getRecentEpisodeFlow(limits: Int): Flow<List<Episode>>

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
    fun getLastPlayedItemIdFlow(): Flow<List<PlayItemId>>

    // PlayItemに含まれる channel, episode を登録する。
    // feedUrl と guid で同じデータが既に登録されているかを確認する。
    // 登録されていたらidと状態以外を更新する。
    @Transaction
    suspend fun addPlayItem(playItem: PlayItem): PlayItem {
        val channel1 = playItem.channel
        // feedUrlで既に登録されているかを調べる。
        val channel2 = getChannelByFeedUrl(channel1.feedUrl)
        // 登録されていたらidと状態取得する。そうでなければそのまま。
        val channel3 = channel2?.let {
            channel1.copy(
                id = it.id,
                subscribed = it.subscribed,
                lastUpdate = it.lastUpdate,
            )
        } ?: channel1
        // 登録されていたらid以外は新しいデータで置き換える。そうでなければそのまま。
        val channelId = safeUpsertChannel(channel3)
        val channel = channel3.copy(id = channelId)

        val item1 = playItem.episode
        // guidで既に登録されているかを調べる。
        val item2 = getEpisodeByGuid(item1.guid)
        // 登録されていたらid, 状態を取得する。そうでなければそのまま。
        val item3 =
            item2?.let {
                item1.copy(
                    id = it.id,
                    playbackPosition = it.playbackPosition,
                    duration = it.duration,
                    lastPlayed = it.lastPlayed
                )
            } ?: item1
        // channelIdをコピーする。
        val item4 = item3.copy(channelId = channelId)
        // 登録されていたらid以外は新しいデータで置き換える。そうでなければそのまま。
        val itemId = safeUpsertEpisode(item4)
        val item = item4.copy(id = itemId)

        // 現在再生しているPlayItemを登録する。
        val playItemId = PlayItemId(channelId = channelId, itemId = itemId)
        safeUpsertPlayItemId(playItemId)

        return PlayItem(channel = channel, episode = item)
    }

    @Query("select * from PChannel where feedUrl = :feedUrl")
    suspend fun getPodCastByFeedUrl(feedUrl: String): PodCast?

    fun recentlyListenedFlow(limits: Int): Flow<List<PlayItem>> {
        return getRecentEpisodeFlow(limits).map { list ->
            list.mapNotNull { ep ->
                getChannelById(ep.channelId)?.let {
                    PlayItem(it, ep)
                }
            }
        }
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "PodCastDao"
    }
}
