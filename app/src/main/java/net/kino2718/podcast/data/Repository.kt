package net.kino2718.podcast.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.kino2718.podcast.db.PodCastDatabase
import java.io.File
import kotlin.time.Duration.Companion.days

class Repository(context: Context) {
    private val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun subscribe(channel: PChannel, subscribe: Boolean) =
        podCastDao.subscribe(channel, subscribe)

    suspend fun upsertPlayItem(playItem: PlayItem): PlayItem = podCastDao.upsertPlayItem(playItem)
    suspend fun setCurrentPlayItem(playItem: PlayItem): PlayItem {
        // 登録を全て消し１つのみ登録する様にする。
        deleteAllPlayItem()
        return podCastDao.upsertCurrentPlayItem(playItem)
    }

    suspend fun addToPlaylist(playItem: PlayItem) = podCastDao.addToPlaylist(playItem)
    private suspend fun deleteAllPlayItem() = podCastDao.deleteAllPlayItems()
    suspend fun getChannelById(id: Long) = podCastDao.getChannelById(id)
    fun getChannelByIdFlow(id: Long) = podCastDao.getChannelByIdFlow(id)
    suspend fun getEpisodeById(id: Long) = podCastDao.getEpisodeById(id)
    fun getEpisodeByIdFlow(id: Long) = podCastDao.getEpisodeByIdFlow(id).distinctUntilChanged()
    fun getLastPlayedItemIdFlow() = podCastDao.getLastPlayedItemIdFlow()

    suspend fun updatePlaybackInfos(
        id: Long, position: Long, duration: Long, completed: Boolean, lastPlayed: Instant
    ) = podCastDao.updatePlaybackInfos(id, position, duration, completed, lastPlayed)

    suspend fun updateDownloadFile(id: Long, file: String?) =
        podCastDao.updateDownloadFile(id, file)

    fun getPodCastByFeedUrlFlow(feedUrl: String) =
        podCastDao.getPodCastByFeedUrlFlow(feedUrl).distinctUntilChanged()

    fun subscribedChannelFlow() = podCastDao.subscribedChannelFlow()
    fun getRecentPlaysFlow(limits: Int) = podCastDao.getRecentPlaysFlow(limits)
    fun getLastPlayedItemFlow() = podCastDao.getLastPlayedItemFlow()
    fun getLatestCompletedItemFlow() =
        podCastDao.getLatestCompletedItemFlow()

    fun getPlaylistItemsFlow() = podCastDao.getPlaylistItemsFlow().distinctUntilChanged()
    suspend fun getPlaylistItems() = podCastDao.getPlaylistItems()
    suspend fun deleteAllPlaylistItems() = podCastDao.deleteAllPlaylistItems()
    suspend fun deletePlaylistItem(playlistItem: PlaylistItem) =
        podCastDao.deletePlaylistItem(playlistItem)

    suspend fun deleteOldDownloadFiles() {
        podCastDao.getDownloadedEpisodes().forEach { episode ->
            val now = Clock.System.now()
            episode.lastPlayed?.let { lastPlayed ->
                if (28.days < now - lastPlayed) {
                    podCastDao.updateDownloadFile(episode.id, null)
                    episode.downloadFile?.let { File(it).delete() }
                }
            }
        }
    }

    fun getEpisodesByGuidsFlow(guids: List<String>): Flow<List<Episode>> {
        return if (guids.isEmpty()) flowOf(listOf())
        else podCastDao.getEpisodesByGuidsFlow(guids)
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "Repository"
    }
}
