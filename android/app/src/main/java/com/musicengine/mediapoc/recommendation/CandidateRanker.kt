package com.musicengine.mediapoc.recommendation

import com.musicengine.mediapoc.db.entity.PlayEventEntity
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.db.entity.TransitionEntity
import com.musicengine.mediapoc.model.CandidateSource
import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.ScoreBreakdown
import com.musicengine.mediapoc.model.ScoringMath
import com.musicengine.mediapoc.model.ScoredCandidate
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.repository.MusicDatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CandidateRanker(
    private val repository: MusicDatabaseRepository
) {

    private data class RankingContext(
        val tracks: Map<String, TrackEntity>,
        val transitions: Map<String, TransitionEntity>,
        val penalties: Map<String, Float>,
        val recentKeyIndexMap: Map<String, Int>
    )

    /**
     * Scores and ranks a list of candidates against the active seed track.
     * Loads all DB data for the candidate pool in a few batched queries instead
     * of issuing per-candidate queries (N+1).
     */
    suspend fun rankCandidates(
        seedTrack: TrackMetadata,
        candidates: List<CandidateTrack>,
        recentEvents: List<PlayEventEntity>? = null
    ): List<ScoredCandidate> = withContext(Dispatchers.Default) {
        val context = buildRankingContext(seedTrack, candidates, recentEvents)
        candidates
            .map { ScoredCandidate(it, scoreSingleCandidate(seedTrack, it, context)) }
            .sortedByDescending { it.breakdown.totalScore }
    }

    private suspend fun buildRankingContext(
        seedTrack: TrackMetadata,
        candidates: List<CandidateTrack>,
        recentEvents: List<PlayEventEntity>?
    ): RankingContext {
        val keys = candidates.map { it.trackKey }.distinct()
        val recent = recentEvents ?: repository.getRecentEvents(limit = 15)
        return RankingContext(
            tracks = repository.getTracksByKeys(keys),
            transitions = repository.getTransitionsFromTo(seedTrack.trackKey, keys),
            penalties = repository.getEffectivePenaltiesByKeys(keys),
            recentKeyIndexMap = recent.mapIndexed { index, event -> event.trackKey to index }.toMap()
        )
    }

    private fun scoreSingleCandidate(
        seedTrack: TrackMetadata,
        candidate: CandidateTrack,
        context: RankingContext
    ): ScoreBreakdown {
        val dbTrack = context.tracks[candidate.trackKey]

        // 1. Long-Term Personal Preference (0 to 35 pts)
        var longTermScore = 0f
        var replayBoost = 0f
        if (dbTrack != null) {
            if (dbTrack.totalPlays > 0) {
                val completionRate = dbTrack.totalCompletions.toFloat() / dbTrack.totalPlays.toFloat()
                longTermScore += (completionRate * 18.0f)
            }
            if (dbTrack.userRating == UserRating.LIKED) {
                longTermScore += 12.0f
            }
            replayBoost = (dbTrack.totalReplays * 3.0f).coerceAtMost(10.0f)
        } else {
            // Unheard catalog track base affinity
            longTermScore = 8.0f
        }

        // 2. Markov Transition Quality S(A -> C) (-30 to +30 pts)
        val transitionPoints = context.transitions[candidate.trackKey]?.let { it.transitionScore * 30.0f } ?: 0f

        // 3. Artist Affinity & Source Boost (0 to 20 pts)
        var artistScore = 0f
        if (candidate.artist.equals(seedTrack.artist, ignoreCase = true)) {
            artistScore += 12.0f
        }
        when (candidate.source) {
            CandidateSource.TRANSITION_HISTORY -> artistScore += 8.0f
            CandidateSource.CATALOG_ARTIST_TOP -> artistScore += 6.0f
            CandidateSource.PERSONAL_LIBRARY -> artistScore += 4.0f
            CandidateSource.CATALOG_SEARCH -> artistScore += 2.0f
            CandidateSource.EXPLORATION -> artistScore += 2.0f
        }

        // 4. Genre Affinity (0 to 8 pts) - only when both the seed and candidate expose a genre
        var genreAffinity = 0f
        val seedGenre = seedTrack.genre
        if (candidate.genre.isNotBlank() && seedGenre.isNotBlank() &&
            candidate.genre.equals(seedGenre, ignoreCase = true)
        ) {
            genreAffinity = 8.0f
        }

        // 5. Novelty / Discovery Boost
        val noveltyBoost = if (candidate.source == CandidateSource.EXPLORATION || dbTrack == null) 5.0f else 0f

        // 6. Decaying Skip Penalty (Subtracted)
        val skipPenalty = context.penalties[candidate.trackKey] ?: 0f

        // 7. Recency Penalty (Subtracted if heard within the last 15 songs)
        var recencyPenalty = 0f
        val recentIndex = context.recentKeyIndexMap[candidate.trackKey]
        if (recentIndex != null) {
            recencyPenalty = when {
                recentIndex < 5 -> 30.0f  // Very recent
                recentIndex < 10 -> 12.0f // Moderately recent
                else -> 5.0f             // Somewhat recent
            }
        }

        val total = ScoringMath.combinedScore(
            longTermPref = longTermScore,
            transitionPoints = transitionPoints,
            artistAffinity = artistScore,
            genreAffinity = genreAffinity,
            replayBoost = replayBoost,
            noveltyBoost = noveltyBoost,
            skipPenalty = skipPenalty,
            recencyPenalty = recencyPenalty
        )

        return ScoreBreakdown(
            longTermPref = longTermScore,
            transitionScore = transitionPoints,
            artistAffinity = artistScore,
            genreAffinity = genreAffinity,
            replayBoost = replayBoost,
            noveltyBoost = noveltyBoost,
            skipPenalty = skipPenalty,
            recencyPenalty = recencyPenalty,
            totalScore = total
        )
    }
}