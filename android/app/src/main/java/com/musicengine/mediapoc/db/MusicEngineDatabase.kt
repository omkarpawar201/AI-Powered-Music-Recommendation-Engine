package com.musicengine.mediapoc.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.musicengine.mediapoc.db.dao.PlayEventDao
import com.musicengine.mediapoc.db.dao.SkipPenaltyDao
import com.musicengine.mediapoc.db.dao.TrackDao
import com.musicengine.mediapoc.db.dao.TransitionDao
import com.musicengine.mediapoc.db.entity.PlayEventEntity
import com.musicengine.mediapoc.db.entity.SkipPenaltyEntity
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.db.entity.TransitionEntity

@Database(
    entities = [
        TrackEntity::class,
        PlayEventEntity::class,
        TransitionEntity::class,
        SkipPenaltyEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MusicEngineDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playEventDao(): PlayEventDao
    abstract fun transitionDao(): TransitionDao
    abstract fun skipPenaltyDao(): SkipPenaltyDao

    companion object {
        @Volatile
        private var INSTANCE: MusicEngineDatabase? = null

        fun getDatabase(context: Context): MusicEngineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicEngineDatabase::class.java,
                    "music_engine_db"
                )
                    // POC safety net: silently drops data on schema changes.
                    // Before shipping, replace with explicit Migration objects per schema bump
                    // and set exportSchema = true to track the schema history.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
