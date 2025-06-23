package net.kino2718.podcast.data

import android.content.Context
import net.kino2718.podcast.db.PodCastDatabase

class Repository(context: Context) {
    private val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun addPlayItem(playItem: PlayItem): PlayItem {
        // ToDo: 今のところplay listはitem1つのみとする。そのうち複数のplay listをサポートする。
        deleteAllPlayItem()
        return podCastDao.addPlayItem(playItem)
    }

    private suspend fun deleteAllPlayItem() = podCastDao.deleteAllPlayItems()

    fun getChannelByIdFlow(id: Long) = podCastDao.getChannelByIdFlow(id)
    fun getItemByIdFlow(id: Long) = podCastDao.getItemByIdFlow(id)
    suspend fun getItemById(id: Long) = podCastDao.getItemById(id)
    fun getAllPlayItemIdsFlow() = podCastDao.getAllPlayItemIdsFlow()
    suspend fun updateItem(item: Item) = podCastDao.updateItem(item)
}