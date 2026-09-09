package com.musicengine.mediapoc.recommendation

import android.content.Context
import android.util.Log
import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.RecommendationResult
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.repository.MusicDatabaseRepository
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendationEngine(
    private val repository: MusicDatabaseRepository,
    private val candidateGenerator: CandidateGenerator = CandidateGenerator(repository),
    private val ranker: CandidateRanker = CandidateRanker(repository)
) {
    companion object {
        private const val TAG = "RecommendationEngine"

        @Volatile
        private var INSTANCE: RecommendationEngine? = null

        fun getInstance(context: Context): RecommendationEngine {
            return INSTANCE ?: synchronized(this) {
                val repo = MusicDatabaseRepository.getInstance(context)
                INSTANCE ?: RecommendationEngine(repo).also { INSTANCE = it }
            }
        }
    }

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

        // 2. Rank candidates locally and deterministically
        val ranked = ranker.rankCandidates(seedTrack, candidatePool, recentEvents)
        val topList = ranked.take(limit)

        RecommendationResult(
            seedTrackTitle = seedTrack.title,
            seedTrackArtist = seedTrack.artist,
            generatedAt = System.currentTimeMillis(),
            topCandidate = topList.firstOrNull(),
            rankedCandidates = topList,
            candidatePoolSize = candidatePool.size
        )
    }

    /**
     * Dispatches playback command to the active player via MediaSession TransportControls.
     */
    fun playCandidate(candidate: CandidateTrack): Boolean {
        val query = "${candidate.title} ${candidate.artist}".trim()
        Log.i(TAG, "Dispatching recommendation playback: playFromSearch(\"$query\")")
        return MediaNotificationListenerService.playFromSearch(query)
    }
}
