package com.musicengine.mediapoc.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.musicengine.mediapoc.model.UserRating

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val trackKey: String, // format: "$title - $artist"
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val artworkUri: String? = null,
    val totalPlays: Int = 0,
    val totalCompletions: Int = 0,
    val totalEarlySkips: Int = 0,
    val totalLateSkips: Int = 0,
    val totalReplays: Int = 0,
    val userRating: UserRating = UserRating.NONE,
    val sourcePackage: String = "",
    val firstPlayedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = System.currentTimeMillis()
)
