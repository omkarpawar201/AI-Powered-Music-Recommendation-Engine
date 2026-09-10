package com.musicengine.mediapoc.network

import android.util.Log
import com.musicengine.mediapoc.model.CandidateSource
import com.musicengine.mediapoc.model.CandidateTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ITunesCatalogClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val cache: CatalogCache = CatalogCache(),
    private val fetchScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "ITunesCatalogClient"
        private const val BASE_URL = "https://itunes.apple.com/search"

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        @Volatile
        private var INSTANCE: ITunesCatalogClient? = null

        fun getInstance(): ITunesCatalogClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ITunesCatalogClient().also { INSTANCE = it }
            }
        }
    }

    private val inFlight = ConcurrentHashMap<String, Deferred<List<CandidateTrack>>>()

    /**
     * Searches top songs by the given artist from the public iTunes catalog.
     */
    suspend fun searchArtistTopTracks(artistName: String, limit: Int = 15): List<CandidateTrack> {
        if (artistName.isBlank() || artistName.equals("Unknown Artist", ignoreCase = true)) {
            return emptyList()
        }
        val query = artistName.trim()
        val url = "$BASE_URL?term=${encode(query)}&entity=song&limit=$limit&media=music"
        return cachedOrFetch(query to limit) {
            fetchCandidatesFromUrl(url, CandidateSource.CATALOG_ARTIST_TOP)
        }
    }

    /**
     * Searches tracks related to a genre, vibe, or search term from the public iTunes catalog.
     */
    suspend fun searchRelatedTracks(query: String, limit: Int = 15): List<CandidateTrack> {
        if (query.isBlank()) return emptyList()
        val url = "$BASE_URL?term=${encode(query.trim())}&entity=song&limit=$limit&media=music"
        return cachedOrFetch(query.trim() to limit) {
            fetchCandidatesFromUrl(url, CandidateSource.CATALOG_SEARCH)
        }
    }

    /**
     * Returns a cached response when fresh, otherwise fetches exactly once.
     * Concurrent callers for the same term coalesce onto a single in-flight request.
     */
    private suspend fun cachedOrFetch(
        cacheKey: Pair<String, Int>,
        fetch: () -> List<CandidateTrack>
    ): List<CandidateTrack> = withContext(Dispatchers.IO) {
        val key = "${cacheKey.first}|${cacheKey.second}"

        cache.get(key)?.let { return@withContext it }

        // Coalesce concurrent identical requests
        val existing = inFlight[key]
        if (existing != null) {
            return@withContext existing.await()
        }

        val deferred = fetchScope.async(Dispatchers.IO) {
            try {
                val result = fetch()
                // Only successful, non-empty results are cached (errors bypass the cache)
                cache.put(key, result)
                result
            } finally {
                inFlight.remove(key)
            }
        }
        val raced = inFlight.putIfAbsent(key, deferred)
        if (raced != null) {
            deferred.cancel()
            return@withContext raced.await()
        }
        return@withContext deferred.await()
    }

    private fun fetchCandidatesFromUrl(url: String, source: CandidateSource): List<CandidateTrack> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/109.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "iTunes API error: HTTP ${response.code}")
                    return emptyList()
                }

                val bodyString = response.body?.string() ?: return emptyList()
                parseResults(bodyString, source)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from iTunes catalog: ${e.message}")
            emptyList()
        }
    }

    private fun parseResults(jsonString: String, source: CandidateSource): List<CandidateTrack> {
        return try {
            val response = json.decodeFromString<ITunesSearchResponse>(jsonString)
            response.results.mapNotNull { item ->
                val trackName = item.trackName?.trim().orEmpty()
                val artistName = item.artistName?.trim().orEmpty()
                if (trackName.isBlank() || artistName.isBlank()) return@mapNotNull null

                // Upscale artwork from 100x100 to 600x600 for sharp Compose display
                val highResArt = item.artworkUrl100
                    ?.takeIf { it.isNotBlank() }
                    ?.replace("100x100bb.jpg", "600x600bb.jpg")

                CandidateTrack(
                    title = trackName,
                    artist = artistName,
                    album = item.collectionName.orEmpty(),
                    durationMs = item.trackTimeMillis,
                    artworkUri = highResArt,
                    genre = item.primaryGenreName.orEmpty(),
                    source = source
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing iTunes JSON: ${e.message}")
            emptyList()
        }
    }

    private fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }
    }

    @Serializable
    private data class ITunesSearchResponse(
        val resultCount: Int = 0,
        val results: List<ITunesResult> = emptyList()
    )

    @Serializable
    private data class ITunesResult(
        val trackName: String? = null,
        val artistName: String? = null,
        val collectionName: String? = null,
        val trackTimeMillis: Long = 0L,
        val primaryGenreName: String? = null,
        val artworkUrl100: String? = null
    )
}