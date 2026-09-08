package com.musicengine.mediapoc.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicengine.mediapoc.db.entity.TransitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransition(transition: TransitionEntity)

    @Query("SELECT * FROM transitions WHERE fromTrackKey = :fromKey AND toTrackKey = :toKey LIMIT 1")
    suspend fun getTransition(fromKey: String, toKey: String): TransitionEntity?

    @Query("SELECT * FROM transitions WHERE fromTrackKey = :fromKey ORDER BY transitionScore DESC, successCount DESC LIMIT :limit")
    suspend fun getTopTransitionsFrom(fromKey: String, limit: Int = 10): List<TransitionEntity>

    @Query("SELECT * FROM transitions ORDER BY lastUpdated DESC")
    fun getAllTransitionsFlow(): Flow<List<TransitionEntity>>
}
