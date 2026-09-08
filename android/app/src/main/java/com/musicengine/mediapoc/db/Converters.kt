package com.musicengine.mediapoc.db

import androidx.room.TypeConverter
import com.musicengine.mediapoc.model.TelemetryEventType
import com.musicengine.mediapoc.model.UserRating

class Converters {
    @TypeConverter
    fun fromUserRating(rating: UserRating?): String {
        return rating?.name ?: UserRating.NONE.name
    }

    @TypeConverter
    fun toUserRating(value: String?): UserRating {
        return try {
            value?.let { UserRating.valueOf(it) } ?: UserRating.NONE
        } catch (e: IllegalArgumentException) {
            UserRating.NONE
        }
    }

    @TypeConverter
    fun fromTelemetryEventType(type: TelemetryEventType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTelemetryEventType(value: String?): TelemetryEventType? {
        return try {
            value?.let { TelemetryEventType.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
