package com.musicengine.mediapoc.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.musicengine.mediapoc.db.entity.PlayEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayEventDao {
    @Insert
    suspend fun insertPlayEvent(event: PlayEventEntity): Long

    @Query("SELECT * FROM play_events ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 50): List<PlayEventEntity>

    @Query("SELECT * FROM play_events ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int = 50): Flow<List<PlayEventEntity>>

    @Query("SELECT * FROM play_events WHERE trackKey = :trackKey ORDER BY startedAt DESC")
    suspend fun getEventsForTrack(trackKey: String): List<PlayEventEntity>
}
