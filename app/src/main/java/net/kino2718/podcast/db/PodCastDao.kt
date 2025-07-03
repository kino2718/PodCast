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
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.kino2718.podcast.data.CurrentPlayItemId
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.PlaylistItem
import net.kino2718.podcast.data.PodCast

@Dao
interface PodCastDao {
    @Upsert
    suspend fun upsertChannel(channel: PChannel): Long

    suspend fun safeUpsertChannel(channel: PChannel): Long {
        val existing = getChannelByFeedUrl(channel.feedUrl)
        if (existing != null) {
            // 既に登録されている
            val channel2 = channel.copy(id = existing.id)
            upsertChannel(channel2)
            return existing.id
        } else {
            // まだ登録されていない
            return upsertChannel(channel)
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
    fun subscribedChannelFlow(): Flow<List<PChannel>>

    suspend fun subscribe(channel: PChannel, subscribe: Boolean) {
        val channelFromDb = getChannelByFeedUrl(channel.feedUrl)
        val channel2 = channelFromDb?.let { c ->
            // 既に登録されていたらidをコピー
            channel.copy(id = c.id)
        } ?: channel
        val channel3 = channel2.copy(subscribed = subscribe)
        upsertChannel(channel3)
    }

    @Upsert
    suspend fun upsertEpisode(episode: Episode): Long

    suspend fun safeUpsertEpisode(episode: Episode): Long {
        val existing = getEpisodeByGuid(episode.guid)
        if (existing != null) {
            // 既に登録されている
            val episode2 = episode.copy(id = existing.id)
            upsertEpisode(episode2)
            return existing.id
        } else {
            // まだ登録されていない
            return upsertEpisode(episode)
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

    @Upsert
    suspend fun upsertPlayItemId(item: CurrentPlayItemId): Long

    @Query("delete from CurrentPlayItemId")
    suspend fun deleteAllPlayItems()

    @Query("select * from CurrentPlayItemId limit 1")
    fun getLastPlayedItemIdFlow(): Flow<CurrentPlayItemId?>

    // PlayItemに含まれる channel, episode を登録する。
    // feedUrl と guid で同じデータが既に登録されているかを確認する。
    // 登録されていたらidと状態以外を更新する。
    @Transaction
    suspend fun upsertCurrentPlayItem(playItem: PlayItem): PlayItem {
        val playItemWithTime = playItem.copy(
            channel = playItem.channel,
            episode = playItem.episode.copy(lastPlayed = Clock.System.now())
        )
        val savedPlayItem = upsertPlayItem(playItemWithTime)

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
        val channelId = safeUpsertChannel(channel)
        val upsertedChannel = channel.copy(id = channelId)

        val episode = playItem.episode.copy(channelId = channelId)
        val episodeId = safeUpsertEpisode(episode)
        val upsertedEpisode = episode.copy(id = episodeId)

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
        return subscribedChannelFlow().flatMapLatest { channels ->
            val flows = channels.map { channel ->
                getLastPlayedEpisodeByIdFlow(channel.id)
                    .distinctUntilChanged()
                    .map { episode ->
                        episode?.let { PlayItem(channel = channel, episode = it) }
                    }
            }
            combine(flows) { arrays ->
                arrays.toList().filterNotNull()
            }
        }
    }

    @Query(
        "select * from Episode " +
                "where (channelId = :channelId) and (isPlaybackCompleted = true) " +
                "order by pubDate desc limit 1"
    )
    fun getLatestCompletedEpisodeByIdFlow(channelId: Long): Flow<Episode?>

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLatestCompletedItemFlow(): Flow<List<PlayItem>> {
        return subscribedChannelFlow().flatMapLatest { channels ->
            val flows = channels.map { channel ->
                getLatestCompletedEpisodeByIdFlow(channel.id)
                    .distinctUntilChanged()
                    .map { episode ->
                        episode?.let { PlayItem(channel = channel, episode = it) }
                    }
            }
            combine(flows) { arrays ->
                arrays.toList().filterNotNull()
            }
        }
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "PodCastDao"
    }
}
