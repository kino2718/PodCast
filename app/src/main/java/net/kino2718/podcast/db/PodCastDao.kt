package net.kino2718.podcast.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.kino2718.podcast.data.CurrentPlayItemId
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PlaylistItem
import net.kino2718.podcast.data.PodCast
import java.time.Instant

@Dao
interface PodCastDao {
    @Upsert
    suspend fun upsertChannel(channel: PChannel): Long

    // uniqueであるfeedUrlで既登録かを確認しそうならそのidを使用する。
    // idと状態であるsubscribe以外はupsertされる。
    @Transaction
    suspend fun safeUpsertChannel(channel: PChannel): PChannel {
        val existing = getChannelByFeedUrl(channel.feedUrl)
        if (existing != null) {
            // 既に登録されている。状態以外のデータを更新。
            val id = existing.id
            val subscribed = existing.subscribed
            val newChannel = channel.copy(id = id, subscribed = subscribed)
            upsertChannel(newChannel)
            return newChannel
        } else {
            // まだ登録されていない
            val id = upsertChannel(channel)
            return channel.copy(id = id)
        }
    }

    @Query("select * from PChannel where id = :id")
    suspend fun getChannelById(id: Long): PChannel?

    @Query("select * from PChannel where id = :id")
    fun getChannelByIdFlow(id: Long): Flow<PChannel?>

    @Query("select * from PChannel where feedUrl = :feedUrl")
    suspend fun getChannelByFeedUrl(feedUrl: String): PChannel?

    @Query("select * from PChannel where feedUrl = :feedUrl")
    fun getChannelByFeedUrlFlow(feedUrl: String): Flow<PChannel?>


    @Query("select * from PChannel where subscribed = true order by lastUpdate desc")
    suspend fun subscribedChannels(): List<PChannel>

    @Query("select * from PChannel where subscribed = true order by lastUpdate desc")
    fun subscribedChannelsFlow(): Flow<List<PChannel>>

    // uniqueであるfeedUrlで既登録かを確認しそうならそのidを使用する。
    @Transaction
    suspend fun subscribe(channel: PChannel, subscribe: Boolean) {
        val id = getChannelByFeedUrl(channel.feedUrl)?.id ?: 0L
        upsertChannel(channel.copy(id = id, subscribed = subscribe))
    }

    @Transaction
    @Query("update PChannel set lastUpdate = :lastUpdate where id = :id")
    suspend fun updateLastUpdate(id: Long, lastUpdate: Instant): Int

    @Upsert
    suspend fun upsertEpisode(episode: Episode): Long

    @Transaction
    suspend fun safeUpsertEpisode(episode: Episode): Episode {
        val existing = getEpisodeByGuid(episode.guid)
        if (existing != null) {
            // 既に登録されている。状態以外のデータを更新。
            val episode1 = episode.copy(
                id = existing.id,
                downloadFile = existing.downloadFile,
                playbackPosition = existing.playbackPosition,
                isPlaybackCompleted = existing.isPlaybackCompleted,
                lastPlayed = existing.lastPlayed,
            )
            upsertEpisode(episode1)
            return episode1
        } else {
            // まだ登録されていない
            val id = upsertEpisode(episode)
            return episode.copy(id = id)
        }
    }

    @Query(
        "update Episode set playbackPosition = :position, duration = :duration, " +
                "isPlaybackCompleted = :completed, lastPlayed = :lastPlayed " +
                "where id = :id"
    )
    suspend fun updatePlaybackInfos(
        id: Long, position: Long, duration: Long, completed: Boolean, lastPlayed: Instant
    ): Int

    @Query("update Episode set downloadFile = :downloadFile where id = :id")
    suspend fun updateDownloadFile(id: Long, downloadFile: String?): Int

    @Query("select * from Episode where downloadFile is not null")
    suspend fun getDownloadedEpisodes(): List<Episode>

    @Query("select * from Episode where id = :id")
    suspend fun getEpisodeById(id: Long): Episode?

    @Query("select * from Episode where id = :id")
    fun getEpisodeByIdFlow(id: Long): Flow<Episode?>

    @Query("select * from Episode where guid = :guid")
    suspend fun getEpisodeByGuid(guid: String): Episode?

    @Query(
        "select * from Episode " +
                "where (lastPlayed is not null) and (isPlaybackCompleted = false) " +
                "order by lastPlayed desc limit :limits"
    )
    fun getRecentEpisodesFlow(limits: Int): Flow<List<Episode>>

    @Query("select * from Episode where guid in (:guids)")
    fun getEpisodesByGuidsFlow(guids: List<String>): Flow<List<Episode>>

    @Upsert
    suspend fun upsertPlayItemId(item: CurrentPlayItemId): Long

    @Query("delete from CurrentPlayItemId")
    suspend fun deleteAllPlayItems()

    @Query("select * from CurrentPlayItemId limit 1")
    fun getLastPlayedItemIdFlow(): Flow<CurrentPlayItemId?>

    @Query("select * from CurrentPlayItemId limit 1")
    suspend fun getLastPlayedItemId(): CurrentPlayItemId?

    // PlayItemに含まれる channel, episode を登録しidを確定する。
    // そしてCurrentPlayItemIdをそのidで更新する。
    @Transaction
    suspend fun upsertCurrentPlayItem(playItem: PlayItem): PlayItem {
        val savedPlayItem = upsertPlayItem(playItem)

        // 現在再生しているPlayItemを登録する。
        val currentPlayItemId = CurrentPlayItemId(
            channelId = savedPlayItem.channel.id,
            episodeId = savedPlayItem.episode.id,
            inPlaylist = savedPlayItem.inPlaylist,
        )
        upsertPlayItemId(currentPlayItemId)
        return savedPlayItem
    }

    @Query("select * from PlaylistItem order by playOrder")
    suspend fun getPlaylistItems(): List<PlaylistItem>

    @Query("select * from PlaylistItem order by playOrder")
    fun getPlaylistItemsFlow(): Flow<List<PlaylistItem>>

    @Query("delete from PlaylistItem")
    suspend fun deleteAllPlaylistItems()

    @Delete
    suspend fun deletePlaylistItem(playlistItem: PlaylistItem)

    @Upsert
    suspend fun upsertPlaylistItem(item: PlaylistItem): Long

    @Transaction
    suspend fun addToPlaylist(playItem: PlayItem) {
        val playlist = getPlaylistItems()
        val maxOrder = playlist.lastOrNull()?.playOrder ?: -1
        val savedPlayItem = upsertPlayItem(playItem)
        val playlistItem = PlaylistItem(
            playOrder = maxOrder + 1,
            channelId = savedPlayItem.channel.id,
            episodeId = savedPlayItem.episode.id
        )
        upsertPlaylistItem(playlistItem)
    }

    suspend fun upsertPlayItem(playItem: PlayItem): PlayItem {
        val channel = playItem.channel
        val upsertedChannel = safeUpsertChannel(channel)

        val episode = playItem.episode.copy(channelId = upsertedChannel.id)
        val upsertedEpisode = safeUpsertEpisode(episode)

        return PlayItem(channel = upsertedChannel, episode = upsertedEpisode, playItem.inPlaylist)
    }

    @Query("select * from PChannel where feedUrl = :feedUrl")
    fun getPodCastByFeedUrlFlow(feedUrl: String): Flow<PodCast?>

    fun getRecentPlaysFlow(limits: Int): Flow<List<PlayItem>> {
        return getRecentEpisodesFlow(limits)
            .distinctUntilChanged()
            .map { episodes ->
                episodes.mapNotNull { ep ->
                    getChannelById(ep.channelId)?.let {
                        PlayItem(it, ep)
                    }
                }
            }
    }

    @Query(
        "select * from Episode " +
                "where (channelId = :channelId) and (lastPlayed is not null) " +
                "order by lastPlayed desc limit 1"
    )
    fun getLastPlayedEpisodeByIdFlow(channelId: Long): Flow<Episode?>

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLastPlayedItemFlow(): Flow<List<PlayItem>> {
        return subscribedChannelsFlow().flatMapLatest { channels ->
            val flows = channels.map { channel ->
                getLastPlayedEpisodeByIdFlow(channel.id)
                    .distinctUntilChanged()
                    .map { episode ->
                        episode?.let { PlayItem(channel = channel, episode = it) }
                    }
            }
            if (flows.isNotEmpty()) combine(flows) { arrays -> arrays.toList().filterNotNull() }
            else flowOf(listOf())
        }
    }

    @Query(
        "select * from Episode " +
                "where (channelId = :channelId) and (isPlaybackCompleted = true) " +
                "order by pubDate desc limit 1"
    )
    fun getLatestCompletedEpisodeByIdFlow(channelId: Long): Flow<Episode?>

    companion object {
        @Suppress("unused")
        private const val TAG = "PodCastDao"
    }
}
