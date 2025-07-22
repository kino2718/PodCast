package net.kino2718.podcast.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import net.kino2718.podcast.db.PodCastDatabase
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

class Repository(context: Context) {
    private val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun subscribe(channel: PChannel, subscribe: Boolean) =
        podCastDao.subscribe(channel, subscribe)

    suspend fun updateLastUpdate(id: Long, lastUpdate: Instant) =
        podCastDao.updateLastUpdate(id, lastUpdate)

    suspend fun upsertPlayItem(playItem: PlayItem): PlayItem = podCastDao.upsertPlayItem(playItem)
    suspend fun setCurrentPlayItem(playItem: PlayItem): PlayItem =
        podCastDao.upsertCurrentPlayItem(playItem)

    suspend fun clearCurrentPlayItem() {
        if (podCastDao.getAppStates() != null) {
            podCastDao.clearCurrentItemIds()
        }
    }

    suspend fun addToPlaylist(playItem: PlayItem) = podCastDao.addToPlaylist(playItem)
    suspend fun getChannelById(id: Long) = podCastDao.getChannelById(id)
    fun getChannelByIdFlow(id: Long) = podCastDao.getChannelByIdFlow(id)
    suspend fun getEpisodeById(id: Long) = podCastDao.getEpisodeById(id)
    fun getEpisodeByIdFlow(id: Long) = podCastDao.getEpisodeByIdFlow(id).distinctUntilChanged()
    fun getAppStatesFlow() = podCastDao.getAppStatesFlow()
    suspend fun getAppStates() = podCastDao.getAppStates()
    suspend fun updateSpeed(speed: Float) = podCastDao.updateSpeed(speed)

    suspend fun updatePlaybackInfos(
        id: Long, position: Long, duration: Long, completed: Boolean, lastPlayed: Instant
    ) = podCastDao.updatePlaybackInfos(id, position, duration, completed, lastPlayed)

    suspend fun updateDownloadFile(id: Long, file: String?) =
        podCastDao.updateDownloadFile(id, file)

    fun getPodCastByFeedUrlFlow(feedUrl: String) =
        podCastDao.getPodCastByFeedUrlFlow(feedUrl).distinctUntilChanged()

    suspend fun subscribedChannels() = podCastDao.subscribedChannels()
    fun subscribedChannelsFlow() = podCastDao.subscribedChannelsFlow()
    fun getRecentPlaysFlow(limits: Int) = podCastDao.getRecentPlaysFlow(limits)
    fun getLastPlayedItemFlow() = podCastDao.getLastPlayedItemFlow()

    fun getPlaylistItemsFlow() = podCastDao.getPlaylistItemsFlow().distinctUntilChanged()
    suspend fun getPlaylistItems() = podCastDao.getPlaylistItems()
    suspend fun deleteAllPlaylistItems() = podCastDao.deleteAllPlaylistItems()
    suspend fun deletePlaylistItem(playlistItem: PlaylistItem) =
        podCastDao.deletePlaylistItem(playlistItem)

    suspend fun deleteOldDownloadFiles() {
        podCastDao.getDownloadedEpisodes().forEach { episode ->
            val now = Instant.now()
            episode.lastPlayed?.let { lastPlayed ->
                if (28L < ChronoUnit.DAYS.between(lastPlayed, now)) {
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
