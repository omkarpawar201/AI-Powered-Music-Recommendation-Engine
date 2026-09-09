package com.musicengine.mediapoc.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicengine.mediapoc.db.entity.SkipPenaltyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkipPenaltyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPenalty(penalty: SkipPenaltyEntity)

    @Query("SELECT * FROM skip_penalties WHERE trackKey = :trackKey LIMIT 1")
    suspend fun getPenalty(trackKey: String): SkipPenaltyEntity?

    @Query("SELECT * FROM skip_penalties WHERE trackKey IN (:keys)")
    suspend fun getPenaltiesByKeys(keys: List<String>): List<SkipPenaltyEntity>

    @Query("SELECT * FROM skip_penalties ORDER BY skipTimestamp DESC")
    suspend fun getAllPenalties(): List<SkipPenaltyEntity>

    @Query("SELECT * FROM skip_penalties ORDER BY skipTimestamp DESC")
    fun getAllPenaltiesFlow(): Flow<List<SkipPenaltyEntity>>

    @Query("DELETE FROM skip_penalties WHERE trackKey = :trackKey")
    suspend fun deletePenalty(trackKey: String)

    @Query("DELETE FROM skip_penalties WHERE skipTimestamp < :beforeTimestamp")
    suspend fun deleteExpiredPenalties(beforeTimestamp: Long)
}
