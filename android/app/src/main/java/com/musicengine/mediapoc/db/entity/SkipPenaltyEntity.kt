package com.musicengine.mediapoc.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skip_penalties")
data class SkipPenaltyEntity(
    @PrimaryKey
    val trackKey: String,
    val skipTimestamp: Long = System.currentTimeMillis(),
    val initialPenalty: Float = 40.0f,
    val halfLifeHours: Float = 4.0f
)
