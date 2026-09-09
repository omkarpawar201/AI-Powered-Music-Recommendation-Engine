package com.musicengine.mediapoc.recommendation

import android.util.Log
import com.musicengine.mediapoc.db.entity.PlayEventEntity
import com.musicengine.mediapoc.model.CandidateSource
import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.network.ITunesCatalogClient
import com.musicengine.mediapoc.repository.MusicDatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CandidateGenerator(
    private val repository: MusicDatabaseRepository,
    private val catalogClient: ITunesCatalogClient = ITunesCatalogClient.getInstance()
) {
    companion object {
        private const val TAG = "CandidateGenerator"
        private const val MIN_POOL_SIZE_FOR_EXPLORATION = 5
    }

    /**
     * Generates a deduplicated, filtered candidate pool (50-100 tracks) for a given seed track.
     *
     * @param recentEvents Recent play events (most recent first). When provided, it is reused
     *                     across generation and ranking so the DB is queried only once.
     */
    suspend fun generateCandidatePool(
        seedTrack: TrackMetadata,
        includeCatalog: Boolean = true,
        recentEvents: List<PlayEventEntity>? = null
    ): List<CandidateTrack> = withContext(Dispatchers.Default) {
        val eventsForFilter = recentEvents ?: repository.getRecentEvents(limit = 15)
        val rawCandidates = mutableListOf<CandidateTrack>()

        coroutineScope {
            // Tier 1A: Direct Markov Transitions from seed track
            val transitionsDeferred = async(Dispatchers.IO) {
                fetchTransitionCandidates(seedTrack.trackKey)
            }

            // Tier 1B: High-affinity tracks from Personal Listening Library
            val libraryDeferred = async(Dispatchers.IO) {
                fetchPersonalLibraryCandidates(seedTrack.trackKey)
            }

            // Tier 2: Public iTunes Catalog (Artist Top Tracks + Related Album Tracks)
            val catalogDeferred = async(Dispatchers.IO) {
                if (includeCatalog && seedTrack.artist.isNotBlank() && seedTrack.artist != "Unknown Artist") {
                    fetchCatalogCandidates(seedTrack)
                } else {
                    emptyList()
                }
            }

            rawCandidates.addAll(transitionsDeferred.await())
            rawCandidates.addAll(libraryDeferred.await())
            rawCandidates.addAll(catalogDeferred.await())
        }

        var filtered = filterAndDeduplicate(seedTrack, rawCandidates, eventsForFilter)

        // Tier 3: Controlled Exploration - only when the pool is starved, to keep behavior unchanged
        if (includeCatalog && filtered.size < MIN_POOL_SIZE_FOR_EXPLORATION) {
            val exploration = fetchExplorationCandidates(seedTrack)
            if (exploration.isNotEmpty()) {
                filtered = filterAndDeduplicate(seedTrack, filtered + exploration, eventsForFilter)
            }
        }

        filtered
    }

    private suspend fun fetchTransitionCandidates(seedTrackKey: String): List<CandidateTrack> {
        val transitions = repository.getTopTransitionsFrom(seedTrackKey, limit = 15)
        if (transitions.isEmpty()) return emptyList()

        // Only suggest transitions with positive or neutral scores
        val scored = transitions.filter { it.transitionScore >= -0.2f }
        if (scored.isEmpty()) return emptyList()

        // Batch-load rich metadata for all transition targets at once
        val savedTracks = repository.getTracksByKeys(scored.map { it.toTrackKey })

        return scored.map { transition ->
            val saved = savedTracks[transition.toTrackKey]
            CandidateTrack(
                title = saved?.title ?: transition.toTrackKey.substringBeforeLast(" - ").trim(),
                artist = saved?.artist ?: transition.toTrackKey.substringAfterLast(" - ").trim(),
                album = saved?.album ?: "",
                durationMs = saved?.durationMs ?: 0L,
                artworkUri = saved?.artworkUri,
                source = CandidateSource.TRANSITION_HISTORY
            )
        }
    }

    private suspend fun fetchPersonalLibraryCandidates(seedTrackKey: String): List<CandidateTrack> {
        val topTracks = repository.getTopTracksFlow(limit = 40).first()
        return topTracks
            .filter { it.trackKey != seedTrackKey && it.userRating != UserRating.DISLIKED }
            .map {
                CandidateTrack(
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    durationMs = it.durationMs,
                    artworkUri = it.artworkUri,
                    source = CandidateSource.PERSONAL_LIBRARY
                )
            }
    }

    private suspend fun fetchCatalogCandidates(seedTrack: TrackMetadata): List<CandidateTrack> {
        val catalogList = mutableListOf<CandidateTrack>()
        try {
            // Fetch top songs by the active artist
            val artistTop = catalogClient.searchArtistTopTracks(seedTrack.artist, limit = 15)
            catalogList.addAll(artistTop)

            // If album exists, search related tracks
            if (seedTrack.album.isNotBlank() && seedTrack.album != "Unknown Album") {
                val albumTracks = catalogClient.searchRelatedTracks("${seedTrack.artist} ${seedTrack.album}", limit = 10)
                catalogList.addAll(albumTracks)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching catalog candidates: ${e.message}")
        }
        return catalogList
    }

    private suspend fun fetchExplorationCandidates(seedTrack: TrackMetadata): List<CandidateTrack> {
        return try {
            val query = seedTrack.genre.ifBlank { seedTrack.title }
            if (query.isBlank()) return emptyList()
            catalogClient.searchRelatedTracks(query, limit = 10)
                .map { it.copy(source = CandidateSource.EXPLORATION) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching exploration candidates: ${e.message}")
            emptyList()
        }
    }

    private suspend fun filterAndDeduplicate(
        seedTrack: TrackMetadata,
        candidates: List<CandidateTrack>,
        recentEvents: List<PlayEventEntity>
    ): List<CandidateTrack> {
        // Anti-repetition window = most recent 5 songs in the shared event list
        val recentlyPlayedKeys = recentEvents.take(5).map { it.trackKey }.toSet()

        // Batch-load library metadata + penalties once instead of per-candidate
        val candidateKeys = candidates.map { it.trackKey }
        val dbTracks = repository.getTracksByKeys(candidateKeys)
        val penalties = repository.getEffectivePenaltiesByKeys(candidateKeys)

        val uniqueCandidates = LinkedHashMap<String, CandidateTrack>()

        for (candidate in candidates) {
            val key = candidate.trackKey

            // Filter 1: Do not recommend the seed track itself
            if (key.equals(seedTrack.trackKey, ignoreCase = true)) continue

            // Filter 2: Prune recently played tracks
            if (key in recentlyPlayedKeys) continue

            // Filter 3: Prune explicit dislikes
            if (dbTracks[key]?.userRating == UserRating.DISLIKED) continue

            // Filter 4: Prune tracks with heavy active skip penalties (> 15 pts)
            val penalty = penalties[key] ?: 0f
            if (penalty > 15.0f) continue

            // Priority deduplication (TRANSITION_HISTORY > PERSONAL_LIBRARY > CATALOG_ARTIST_TOP > CATALOG_SEARCH > EXPLORATION)
            if (!uniqueCandidates.containsKey(key)) {
                uniqueCandidates[key] = candidate
            } else {
                val existing = uniqueCandidates[key]!!
                if (candidate.source.ordinal < existing.source.ordinal) {
                    uniqueCandidates[key] = candidate
                }
            }
        }

        return uniqueCandidates.values.toList()
    }
}