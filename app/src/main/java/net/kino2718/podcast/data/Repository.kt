package net.kino2718.podcast.data

import android.content.Context
import net.kino2718.podcast.db.PodCastDatabase

class Repository(context: Context) {
    private val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun subscribe(channel: PChannel, subscribe: Boolean) =
        podCastDao.subscribe(channel, subscribe)

    suspend fun addPlayItem(playItem: PlayItem): PlayItem {
        // 登録を全て消し１つのみ登録する様にする。
        deleteAllPlayItem()
        return podCastDao.addPlayItem(playItem)
    }

    suspend fun addToPlaylist(playItem: PlayItem) {
        podCastDao.addToPlaylist(playItem)
    }

    private suspend fun deleteAllPlayItem() = podCastDao.deleteAllPlayItems()

    suspend fun getChannelById(id: Long) = podCastDao.getChannelById(id)
    suspend fun getEpisodeById(id: Long) = podCastDao.getEpisodeById(id)
    fun getLastPlayedItemIdFlow() = podCastDao.getLastPlayedItemIdFlow()
    suspend fun updateEpisode(episode: Episode) = podCastDao.updateEpisode(episode)
    suspend fun getPodCastByFeedUrl(feedUrl: String) = podCastDao.getPodCastByFeedUrl(feedUrl)
    fun subscribedChannelFlow() = podCastDao.subscribedChannelFlow()
    suspend fun subscribedChannels() = podCastDao.subscribedChannels()
    suspend fun getRecentPlays(limits: Int) = podCastDao.getRecentPlays(limits)
    suspend fun getLastPlayedEpisodeByFeedUrl(feedUrl: String) =
        podCastDao.getLastPlayedEpisodeByFeedUrl(feedUrl)

    suspend fun getLatestCompletedEpisodeByFeedUrl(feedUrl: String) =
        podCastDao.getLatestCompletedEpisodeByFeedUrl(feedUrl)

    fun getPlaylistItemsFlow() = podCastDao.getPlaylistItemsFlow()
    suspend fun deleteAllPlaylistItems() = podCastDao.deleteAllPlaylistItems()
    suspend fun deletePlaylistItem(playlistItem: PlaylistItem) =
        podCastDao.deletePlaylistItem(playlistItem)
}

@Suppress("unused")
private const val TAG = "Repository"
