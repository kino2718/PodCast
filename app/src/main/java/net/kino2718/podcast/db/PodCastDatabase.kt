package net.kino2718.podcast.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.kino2718.podcast.data.CurrentPlayItemId
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlaylistItem

@Database(
    entities = [PChannel::class, Episode::class, CurrentPlayItemId::class, PlaylistItem::class],
    version = 1
)
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