package com.musicengine.mediapoc.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.musicengine.mediapoc.model.TelemetryEventType

@Entity(
    tableName = "play_events",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["trackKey"],
            childColumns = ["trackKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("trackKey"),
        Index("startedAt")
    ]
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val trackKey: String,
    val startedAt: Long = System.currentTimeMillis(),
    val durationListenedMs: Long = 0L,
    val completionRatio: Float = 0.0f,
    val eventType: TelemetryEventType,
    val previousTrackKey: String? = null,
    val sessionId: String = ""
)
