package net.kino2718.podcast.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.kino2718.podcast.data.AppStates
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlaylistItem

@Database(
    entities = [PChannel::class, Episode::class, AppStates::class, PlaylistItem::class],
    version = 2
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
            )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `AppStates` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`channelId` INTEGER, " +
                        "`episodeId` INTEGER, " +
                        "`inPlaylist` INTEGER NOT NULL, " +
                        "`speed` REAL NOT NULL DEFAULT 1.0)"
                )
                db.execSQL(
                    "INSERT INTO AppStates (id, channelId, episodeId, inPlaylist, speed) SELECT id, channelId, episodeId, inPlaylist, 1.0 FROM CurrentPlayItemId"
                )
                db.execSQL("DROP TABLE IF EXISTS CurrentPlayItemId")
            }
        }
    }
}
