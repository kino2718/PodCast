package net.kino2718.podcast.data

import android.content.Context
import net.kino2718.podcast.db.PodCastDatabase

class Repository(context: Context) {
    private val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun subscribe(channel: PChannel, subscribe: Boolean) =
        podCastDao.subscribe(channel, subscribe)

    suspend fun setPlayItem(playItem: PlayItem): PlayItem = podCastDao.upsertPlayItem(playItem)
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
    fun getEpisodeByIdFlow(id: Long) = podCastDao.getEpisodeByIdFlow(id)
    fun getLastPlayedItemIdFlow() = podCastDao.getLastPlayedItemIdFlow()
    suspend fun updateEpisode(episode: Episode) = podCastDao.updateEpisode(episode)
    fun getPodCastByFeedUrlFlow(feedUrl: String) = podCastDao.getPodCastByFeedUrlFlow(feedUrl)
    fun subscribedChannelFlow() = podCastDao.subscribedChannelFlow()
    fun getRecentPlaysFlow(limits: Int) = podCastDao.getRecentPlaysFlow(limits)
    fun getLastPlayedItemFlow() = podCastDao.getLastPlayedItemFlow()
    fun getLatestCompletedItemFlow() =
        podCastDao.getLatestCompletedItemFlow()

    fun getPlaylistItemsFlow() = podCastDao.getPlaylistItemsFlow()
    suspend fun getPlaylistItems() = podCastDao.getPlaylistItems()
    suspend fun deleteAllPlaylistItems() = podCastDao.deleteAllPlaylistItems()
    suspend fun deletePlaylistItem(playlistItem: PlaylistItem) =
        podCastDao.deletePlaylistItem(playlistItem)
}

@Suppress("unused")
private const val TAG = "Repository"
