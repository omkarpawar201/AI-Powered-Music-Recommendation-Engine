package com.musicengine.mediapoc.model

import android.graphics.Bitmap
import android.net.Uri

fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

enum class PlaybackStateEnum {
    PLAYING,
    PAUSED,
    BUFFERING,
    STOPPED,
    NONE
}

enum class UserRating {
    NONE,
    LIKED,
    DISLIKED
}

enum class TelemetryEventType {
    TRACK_STARTED,
    NATURAL_COMPLETION,
    SKIP_EARLY,
    SKIP_LATE,
    REPLAY,
    PAUSED,
    RESUMED,
    SEEKED,
    USER_LIKE,
    USER_DISLIKE
}

data class TrackMetadata(
    val title: String = "Unknown Title",
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val genre: String = "",
    val artBitmap: Bitmap? = null,
    val artUri: Uri? = null,
    val mediaId: String? = null,
    val mediaUri: Uri? = null,
    val packageName: String = "",
    val appDisplayName: String = "No App",
    val userRating: UserRating = UserRating.NONE
) {
    val trackKey: String
        get() = "${title.trim()} - ${artist.trim()}"
}

data class PlaybackTelemetryState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val state: PlaybackStateEnum = PlaybackStateEnum.NONE,
    val speed: Float = 1.0f,
    val completionRatio: Float = 0.0f
) {
    val formattedPosition: String
        get() = formatTime(positionMs)

    val formattedDuration: String
        get() = formatTime(durationMs)
}

data class TelemetryEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val type: TelemetryEventType,
    val trackTitle: String,
    val artist: String,
    val listenedDurationMs: Long,
    val completionPercentage: Float,
    val description: String
)
