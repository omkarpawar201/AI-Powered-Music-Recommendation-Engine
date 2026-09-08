package com.musicengine.mediapoc.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "transitions",
    primaryKeys = ["fromTrackKey", "toTrackKey"],
    indices = [
        Index("fromTrackKey"),
        Index("toTrackKey")
    ]
)
data class TransitionEntity(
    val fromTrackKey: String,
    val toTrackKey: String,
    val transitionCount: Int = 0,
    val successCount: Int = 0,
    val earlySkipCount: Int = 0,
    val lateSkipCount: Int = 0,
    val transitionScore: Float = 0.0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
