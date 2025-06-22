package net.kino2718.podcast.data

import android.content.Context
import net.kino2718.podcast.db.PodCastDatabase

class Repository(context: Context) {
    val podCastDao = PodCastDatabase.getInstance(context).podCastDao()

    suspend fun addPlayItem(playItem: PlayItem): PlayItem {
        // ToDo: 今のところplay listはitem1つのみとする。そのうち複数のplay listをサポートする。
        deleteAllPlayItem()
        return podCastDao.addPlayItem(playItem)
    }

    private suspend fun deleteAllPlayItem() = podCastDao.deleteAllPlayItems()

    fun getPlayListFlow() = podCastDao.getPlayListFlow()
}