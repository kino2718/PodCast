package net.kino2718.podcast.data

import android.content.Context
import net.kino2718.podcast.db.PodCastDatabase

class Repository(context: Context) {
    private val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun subscribe(channel: PChannel, subscribe: Boolean) =
        podCastDao.safeUpsertChannel(channel.copy(subscribed = subscribe))

    suspend fun addPlayItem(playItem: PlayItem): PlayItem {
        // 登録を全て消し１つのみ登録する様にする。
        deleteAllPlayItem()
        return podCastDao.addPlayItem(playItem)
    }

    private suspend fun deleteAllPlayItem() = podCastDao.deleteAllPlayItems()

    suspend fun getChannelById(id: Long) = podCastDao.getChannelById(id)
    suspend fun getEpisodeById(id: Long) = podCastDao.getEpisodeById(id)
    fun getLastPlayedItemIdFlow() = podCastDao.getLastPlayedItemIdFlow()
    suspend fun updateEpisode(episode: Episode) = podCastDao.updateEpisode(episode)
    suspend fun getPodCastByFeedUrl(feedUrl: String) = podCastDao.getPodCastByFeedUrl(feedUrl)
    fun subscribedChannelFlow() = podCastDao.subscribedChannelFlow()
    fun recentlyListenedFlow(limits: Int) = podCastDao.recentlyListenedFlow(limits)
}

@Suppress("unused")
private const val TAG = "Repository"
