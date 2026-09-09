package com.musicengine.mediapoc.model

import kotlinx.serialization.Serializable

enum class CandidateSource {
    TRANSITION_HISTORY,     // Tier 1: Direct Markov transition from current track (A -> B)
    PERSONAL_LIBRARY,       // Tier 1: High completion/replay/like tracks in Room DB
    CATALOG_ARTIST_TOP,     // Tier 2: Public iTunes catalog top tracks by artist
    CATALOG_SEARCH,         // Tier 2: Public iTunes catalog search
    EXPLORATION             // Tier 3: Controlled discovery/exploration pool
}

@Serializable
data class CandidateTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val artworkUri: String? = null,
    val genre: String = "",
    val source: CandidateSource = CandidateSource.PERSONAL_LIBRARY
) {
    val trackKey: String
        get() = "${title.trim()} - ${artist.trim()}"
}

data class ScoreBreakdown(
    val longTermPref: Float = 0f,
    val transitionScore: Float = 0f,
    val artistAffinity: Float = 0f,
    val genreAffinity: Float = 0f,
    val replayBoost: Float = 0f,
    val noveltyBoost: Float = 0f,
    val skipPenalty: Float = 0f,
    val recencyPenalty: Float = 0f,
    val totalScore: Float = 0f
)

data class ScoredCandidate(
    val track: CandidateTrack,
    val breakdown: ScoreBreakdown
)

data class RecommendationResult(
    val seedTrackTitle: String,
    val seedTrackArtist: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val topCandidate: ScoredCandidate? = null,
    val rankedCandidates: List<ScoredCandidate> = emptyList(),
    val candidatePoolSize: Int = 0
)
