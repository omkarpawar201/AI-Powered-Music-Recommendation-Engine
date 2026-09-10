package com.musicengine.mediapoc.recommendation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.musicengine.mediapoc.model.CandidateSource
import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.RecommendationResult
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.network.NetworkUtils
import com.musicengine.mediapoc.repository.MusicDatabaseRepository
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendationEngine(
    private val repository: MusicDatabaseRepository,
    private val candidateGenerator: CandidateGenerator = CandidateGenerator(repository),
    private val ranker: CandidateRanker = CandidateRanker(repository),
    private val appContext: Context? = null,
    private val hasNetwork: () -> Boolean = { true },
    private val diversityMaxPerArtist: Int = DEFAULT_MAX_PER_ARTIST
) {
    companion object {
        private const val TAG = "RecommendationEngine"
        private const val DEFAULT_MAX_PER_ARTIST = 2
        // Ignore duplicate playback dispatches for the same track within this window
        private const val DEDUP_DISPATCH_WINDOW_MS = 5000L

        @Volatile
        private var INSTANCE: RecommendationEngine? = null

        fun getInstance(context: Context): RecommendationEngine {
            return INSTANCE ?: synchronized(this) {
                val app = context.applicationContext
                val repo = MusicDatabaseRepository.getInstance(context)
                INSTANCE ?: RecommendationEngine(
                    repository = repo,
                    appContext = app,
                    hasNetwork = { NetworkUtils.isOnline(app) }
                ).also { INSTANCE = it }
            }
        }
    }

    private val dispatchLock = Any()
    @Volatile private var dispatchInFlight = false
    @Volatile private var lastTargetKey: String? = null
    @Volatile private var lastDispatchAt = 0L

    /**
     * Executes the full recommendation pipeline:
     * Current Track -> Candidate Pool (Tier 1, 2, 3) -> Filtering -> Local Multi-Factor Ranking -> Ranked Result
     */
    suspend fun getRecommendations(
        seedTrack: TrackMetadata,
        limit: Int = 15
    ): RecommendationResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "Generating recommendations for seed: ${seedTrack.title} by ${seedTrack.artist}")

        // Fetch recent events once and reuse across generation (anti-repetition window)
        // and ranking (recency penalty) to avoid redundant DB queries.
        val recentEvents = repository.getRecentEvents(limit = 15)

        // 1. Generate candidate pool
        val candidatePool = candidateGenerator.generateCandidatePool(
            seedTrack = seedTrack,
            recentEvents = recentEvents
        )

        // Catalog-dependent sources are the only ones that need a network connection;
        // when offline they are empty, so recommendations degrade to on-device-only.
        val offlineUsed = !hasNetwork() && candidatePool.none { it.isCatalogSource }

        // 2. Rank candidates locally and deterministically
        val ranked = ranker.rankCandidates(seedTrack, candidatePool, recentEvents)
        val diversified = DiversityFilter.apply(ranked, diversityMaxPerArtist)
        val topList = diversified.take(limit)

        RecommendationResult(
            seedTrackTitle = seedTrack.title,
            seedTrackArtist = seedTrack.artist,
            generatedAt = System.currentTimeMillis(),
            topCandidate = topList.firstOrNull(),
            rankedCandidates = topList,
            candidatePoolSize = candidatePool.size,
            offlineUsed = offlineUsed
        )
    }

    /**
     * Dispatches playback command to the active player via MediaSession TransportControls,
     * with a structured query + auto-fallback to an ACTION_MEDIA_PLAY_FROM_SEARCH intent.
     * Guards against duplicate/overlapping dispatches.
     */
    fun playCandidate(candidate: CandidateTrack): Boolean {
        val now = System.currentTimeMillis()

        // Ignore rapid duplicate triggers for the same track (autoplay safety)
        if (candidate.trackKey == lastTargetKey && now - lastDispatchAt < DEDUP_DISPATCH_WINDOW_MS) {
            Log.w(TAG, "Suppressing duplicate dispatch for ${candidate.trackKey}")
            return true
        }

        synchronized(dispatchLock) {
            if (dispatchInFlight) {
                Log.w(TAG, "Dispatch already in flight; ignoring")
                return false
            }
            dispatchInFlight = true
        }
        lastTargetKey = candidate.trackKey
        lastDispatchAt = now

        return try {
            val query = "${candidate.title} ${candidate.artist}".trim()
            val extras = Bundle().apply {
                putString(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
                putString(MediaStore.EXTRA_MEDIA_TITLE, candidate.title)
                putString(MediaStore.EXTRA_MEDIA_ARTIST, candidate.artist)
            }
            Log.i(TAG, "Dispatching recommendation playback: playFromSearch(\"$query\")")

            if (MediaNotificationListenerService.playFromSearch(query, extras)) {
                true
            } else {
                fallbackToSearchIntent(candidate)
            }
        } finally {
            dispatchInFlight = false
        }
    }

    private fun fallbackToSearchIntent(candidate: CandidateTrack): Boolean {
        val ctx = appContext ?: return false
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
                putExtra(MediaStore.EXTRA_MEDIA_TITLE, candidate.title)
                putExtra(MediaStore.EXTRA_MEDIA_ARTIST, candidate.artist)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            Log.i(TAG, "Fallback: dispatched ACTION_MEDIA_PLAY_FROM_SEARCH intent")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback intent failed", e)
            false
        }
    }
}