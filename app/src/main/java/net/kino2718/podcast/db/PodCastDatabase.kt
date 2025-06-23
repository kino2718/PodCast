package net.kino2718.podcast.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.kino2718.podcast.data.Item
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItemId

@Database(entities = [PChannel::class, Item::class, PlayItemId::class], version = 1)
@TypeConverters(DataConverter::class)
abstract class PodCastDatabase : RoomDatabase() {
    abstract fun podCastDao(): PodCastDao

    companion object {
        @Volatile
        private var instance: PodCastDatabase? = null

        fun getInstance(context: Context): PodCastDatabase {
            instance?.let { return it }

            return Room.databaseBuilder(
                context.applicationContext,
                PodCastDatabase::class.java,
                "database.db"
            ).build().also { instance = it }
        }
    }
}