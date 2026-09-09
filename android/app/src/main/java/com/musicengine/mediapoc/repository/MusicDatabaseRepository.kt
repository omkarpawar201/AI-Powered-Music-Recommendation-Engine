package com.musicengine.mediapoc.repository

import android.content.Context
import com.musicengine.mediapoc.db.MusicEngineDatabase
import com.musicengine.mediapoc.db.entity.PlayEventEntity
import com.musicengine.mediapoc.db.entity.SkipPenaltyEntity
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.db.entity.TransitionEntity
import com.musicengine.mediapoc.model.TelemetryEventType
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.model.UserRating
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.pow

class MusicDatabaseRepository(
    private val db: MusicEngineDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val trackDao = db.trackDao()
    private val playEventDao = db.playEventDao()
    private val transitionDao = db.transitionDao()
    private val skipPenaltyDao = db.skipPenaltyDao()

    // ─── Mathematical Engines ───────────────────────────────────────────────

    /**
     * Exponential Half-Life Decay Calculator:
     * P(t) = P0 * (1/2) ^ (elapsedTime / halfLife)
     */
    fun calculateEffectivePenalty(
        initialPenalty: Float,
        skipTimestamp: Long,
        halfLifeHours: Float,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Float {
        val elapsedMs = (currentTimestamp - skipTimestamp).coerceAtLeast(0L)
        val elapsedHours = elapsedMs / (1000.0 * 3600.0)
        val halfLife = halfLifeHours.toDouble().coerceAtLeast(0.1)
        val decayFactor = (0.5).pow(elapsedHours / halfLife)
        return (initialPenalty * decayFactor).toFloat()
    }

    /**
     * Laplace-Smoothed Markov Transition Score:
     * S(A -> B) = (successes - 1.5 * earlySkips - 0.5 * lateSkips) / (totalTransitions + 2.0)
     * Normalized between -1.0 and +1.0.
     */
    fun calculateTransitionScore(
        successCount: Int,
        earlySkipCount: Int,
        lateSkipCount: Int,
        totalTransitions: Int
    ): Float {
        val rawNumerator = successCount.toDouble() - (1.5 * earlySkipCount) - (0.5 * lateSkipCount)
        val denominator = totalTransitions.toDouble() + 2.0 // Laplace smoothing (alpha = 2)
        val score = (rawNumerator / denominator).toFloat()
        return score.coerceIn(-1.0f, 1.0f)
    }

    // ─── Track & Library Operations ─────────────────────────────────────────

    fun getAllTracksFlow(): Flow<List<TrackEntity>> = trackDao.getAllTracksFlow()

    fun getTopTracksFlow(limit: Int = 20): Flow<List<TrackEntity>> = trackDao.getTopTracksFlow(limit)

    fun getTotalTrackCountFlow(): Flow<Int> = trackDao.getTotalTrackCountFlow()

    suspend fun getTrack(trackKey: String): TrackEntity? = withContext(ioDispatcher) {
        trackDao.getTrack(trackKey)
    }

    suspend fun recordTrackStart(metadata: TrackMetadata): TrackEntity = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val existing = trackDao.getTrack(metadata.trackKey)
        val track = if (existing != null) {
            existing.copy(
                album = metadata.album.ifBlank { existing.album },
                durationMs = if (metadata.durationMs > 0) metadata.durationMs else existing.durationMs,
                artworkUri = metadata.artUri?.toString() ?: existing.artworkUri,
                sourcePackage = metadata.packageName.ifBlank { existing.sourcePackage },
                totalPlays = existing.totalPlays + 1,
                lastPlayedAt = now
            )
        } else {
            TrackEntity(
                trackKey = metadata.trackKey,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                durationMs = metadata.durationMs,
                artworkUri = metadata.artUri?.toString(),
                totalPlays = 1,
                userRating = metadata.userRating,
                sourcePackage = metadata.packageName,
                firstPlayedAt = now,
                lastPlayedAt = now
            )
        }
        trackDao.upsertTrack(track)
        track
    }

    suspend fun incrementCompletion(trackKey: String) = withContext(ioDispatcher) {
        trackDao.incrementCompletionCount(trackKey)
    }

    suspend fun incrementEarlySkip(trackKey: String) = withContext(ioDispatcher) {
        trackDao.incrementEarlySkipCount(trackKey)
    }

    suspend fun incrementLateSkip(trackKey: String) = withContext(ioDispatcher) {
        trackDao.incrementLateSkipCount(trackKey)
    }

    suspend fun incrementReplay(trackKey: String) = withContext(ioDispatcher) {
        trackDao.incrementReplayCount(trackKey)
    }

    suspend fun updateUserRating(trackKey: String, rating: UserRating) = withContext(ioDispatcher) {
        trackDao.updateUserRating(trackKey, rating)
    }

    // ─── Play Event Logging ─────────────────────────────────────────────────

    fun getRecentEventsFlow(limit: Int = 50): Flow<List<PlayEventEntity>> =
        playEventDao.getRecentEventsFlow(limit)

    suspend fun logPlayEvent(
        trackKey: String,
        startedAt: Long,
        durationListenedMs: Long,
        completionRatio: Float,
        eventType: TelemetryEventType,
        previousTrackKey: String? = null,
        sessionId: String = ""
    ): Long = withContext(ioDispatcher) {
        playEventDao.insertPlayEvent(
            PlayEventEntity(
                trackKey = trackKey,
                startedAt = startedAt,
                durationListenedMs = durationListenedMs,
                completionRatio = completionRatio,
                eventType = eventType,
                previousTrackKey = previousTrackKey,
                sessionId = sessionId
            )
        )
    }

    // ─── Transitions (A -> B) Operations ────────────────────────────────────

    fun getAllTransitionsFlow(): Flow<List<TransitionEntity>> =
        transitionDao.getAllTransitionsFlow()

    suspend fun recordTransitionEvent(
        fromTrackKey: String,
        toTrackKey: String,
        isSuccess: Boolean,
        isEarlySkip: Boolean,
        isLateSkip: Boolean
    ) = withContext(ioDispatcher) {
        if (fromTrackKey.isBlank() || toTrackKey.isBlank() || fromTrackKey == toTrackKey) return@withContext

        val existing = transitionDao.getTransition(fromTrackKey, toTrackKey)
        val newCount = (existing?.transitionCount ?: 0) + 1
        val newSuccess = (existing?.successCount ?: 0) + (if (isSuccess) 1 else 0)
        val newEarly = (existing?.earlySkipCount ?: 0) + (if (isEarlySkip) 1 else 0)
        val newLate = (existing?.lateSkipCount ?: 0) + (if (isLateSkip) 1 else 0)

        val newScore = calculateTransitionScore(
            successCount = newSuccess,
            earlySkipCount = newEarly,
            lateSkipCount = newLate,
            totalTransitions = newCount
        )

        val entity = TransitionEntity(
            fromTrackKey = fromTrackKey,
            toTrackKey = toTrackKey,
            transitionCount = newCount,
            successCount = newSuccess,
            earlySkipCount = newEarly,
            lateSkipCount = newLate,
            transitionScore = newScore,
            lastUpdated = System.currentTimeMillis()
        )
        transitionDao.upsertTransition(entity)
    }

    // ─── Skip Penalty Operations ────────────────────────────────────────────

    fun getAllPenaltiesFlow(): Flow<List<SkipPenaltyEntity>> =
        skipPenaltyDao.getAllPenaltiesFlow()

    suspend fun applySkipPenalty(
        trackKey: String,
        initialPenalty: Float = 40.0f,
        halfLifeHours: Float = 4.0f
    ) = withContext(ioDispatcher) {
        val penalty = SkipPenaltyEntity(
            trackKey = trackKey,
            skipTimestamp = System.currentTimeMillis(),
            initialPenalty = initialPenalty,
            halfLifeHours = halfLifeHours
        )
        skipPenaltyDao.upsertPenalty(penalty)
    }

    suspend fun getEffectivePenaltyForTrack(trackKey: String): Float = withContext(ioDispatcher) {
        val record = skipPenaltyDao.getPenalty(trackKey) ?: return@withContext 0.0f
        val effective = calculateEffectivePenalty(
            initialPenalty = record.initialPenalty,
            skipTimestamp = record.skipTimestamp,
            halfLifeHours = record.halfLifeHours
        )
        if (effective < 1.0f) {
            skipPenaltyDao.deletePenalty(trackKey)
            0.0f
        } else {
            effective
        }
    }

    suspend fun cleanupExpiredPenalties() = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val penalties = skipPenaltyDao.getAllPenalties()
        penalties.forEach { penalty ->
            val effective = calculateEffectivePenalty(
                initialPenalty = penalty.initialPenalty,
                skipTimestamp = penalty.skipTimestamp,
                halfLifeHours = penalty.halfLifeHours,
                currentTimestamp = now
            )
            if (effective < 1.0f) {
                skipPenaltyDao.deletePenalty(penalty.trackKey)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabaseRepository? = null

        fun getInstance(context: Context): MusicDatabaseRepository {
            return INSTANCE ?: synchronized(this) {
                val db = MusicEngineDatabase.getDatabase(context)
                val instance = MusicDatabaseRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
