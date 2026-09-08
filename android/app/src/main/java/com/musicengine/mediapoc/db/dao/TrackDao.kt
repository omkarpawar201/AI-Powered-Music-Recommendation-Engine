package com.musicengine.mediapoc.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.model.UserRating
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE trackKey = :trackKey LIMIT 1")
    suspend fun getTrack(trackKey: String): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY totalPlays DESC, totalCompletions DESC LIMIT :limit")
    fun getTopTracksFlow(limit: Int = 20): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET userRating = :rating WHERE trackKey = :trackKey")
    suspend fun updateUserRating(trackKey: String, rating: UserRating)

    @Query("UPDATE tracks SET totalPlays = totalPlays + 1, lastPlayedAt = :timestamp WHERE trackKey = :trackKey")
    suspend fun incrementPlayCount(trackKey: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET totalCompletions = totalCompletions + 1 WHERE trackKey = :trackKey")
    suspend fun incrementCompletionCount(trackKey: String)

    @Query("UPDATE tracks SET totalEarlySkips = totalEarlySkips + 1 WHERE trackKey = :trackKey")
    suspend fun incrementEarlySkipCount(trackKey: String)

    @Query("UPDATE tracks SET totalLateSkips = totalLateSkips + 1 WHERE trackKey = :trackKey")
    suspend fun incrementLateSkipCount(trackKey: String)

    @Query("UPDATE tracks SET totalReplays = totalReplays + 1 WHERE trackKey = :trackKey")
    suspend fun incrementReplayCount(trackKey: String)

    @Query("SELECT COUNT(*) FROM tracks")
    fun getTotalTrackCountFlow(): Flow<Int>
}
