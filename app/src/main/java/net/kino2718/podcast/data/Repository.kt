package net.kino2718.podcast.data

import android.content.Context
import net.kino2718.podcast.db.PodCastDatabase

class Repository(context: Context) {
    private val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun subscribe(channel: PChannel) =
        podCastDao.safeUpsertChannel(channel.copy(subscribed = true))

    suspend fun addLastPlayedItem(playItem: PlayItem): PlayItem {
        // 登録を全て消し１つのみ登録する様にする。
        deleteAllPlayItem()
        return podCastDao.addPlayItem(playItem)
    }

    private suspend fun deleteAllPlayItem() = podCastDao.deleteAllPlayItems()

    fun getChannelByIdFlow(id: Long) = podCastDao.getChannelByIdFlow(id)
    fun getEpisodeByIdFlow(id: Long) = podCastDao.getEpisodeByIdFlow(id)
    fun getLastPlayedItemIdFlow() = podCastDao.getLastPlayedItemIdFlow()
    suspend fun updateEpisode(episode: Episode) = podCastDao.updateEpisode(episode)
    fun getPodCastFlowByFeedUrl(feedUrl: String) = podCastDao.getPodCastFlowByFeedUrl(feedUrl)
    fun subscribedChannelFlow() = podCastDao.subscribedChannelFlow()
}

@Suppress("unused")
private const val TAG = "Repository"
