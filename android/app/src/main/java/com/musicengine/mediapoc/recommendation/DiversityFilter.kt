package com.musicengine.mediapoc.recommendation

import com.musicengine.mediapoc.model.ScoredCandidate

/**
 * Order-stable diversity pass over an already-ranked candidate list.
 * Caps how many recommendations may share the same artist, keeping the
 * top-N from becoming an album playback of the seed artist. Pass
 * [maxPerArtist] <= 0 to disable (returns the input unchanged).
 */
object DiversityFilter {

    fun apply(candidates: List<ScoredCandidate>, maxPerArtist: Int): List<ScoredCandidate> {
        if (maxPerArtist <= 0 || candidates.size <= 1) return candidates

        val counts = HashMap<String, Int>(candidates.size)
        val result = ArrayList<ScoredCandidate>(candidates.size)

        for (candidate in candidates) {
            val artist = candidate.track.artist.trim().lowercase()
            val count = counts[artist] ?: 0
            if (count < maxPerArtist) {
                result.add(candidate)
                counts[artist] = count + 1
            }
        }
        return result
    }
}