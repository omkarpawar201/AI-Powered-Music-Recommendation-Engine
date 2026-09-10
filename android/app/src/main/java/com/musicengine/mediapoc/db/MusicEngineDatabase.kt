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
    version = 2,
    exportSchema = true
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
                    // Strict migrations only — every schema change must ship a Migration object.
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // v1 -> v2: canonical track key re-keying + new indices
        private val MIGRATION_1_2 = MigrationV1ToV2()
    }
}
