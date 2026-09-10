package com.musicengine.mediapoc.network

import com.musicengine.mediapoc.model.CandidateTrack
import java.util.LinkedHashMap

/**
 * Small in-memory LRU cache for iTunes catalog responses.
 * Only successful, non-empty results are cached; transient network errors are not,
 * so a failed request gets a fresh retry instead of a stale TTL block.
 */
class CatalogCache(
    private val maxSize: Int = 64,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Entry(val fetchedAt: Long, val candidates: List<CandidateTrack>)

    private val cache = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: String): List<CandidateTrack>? {
        val entry = cache[key] ?: return null
        if (now() - entry.fetchedAt > ttlMillis) {
            cache.remove(key)
            return null
        }
        return entry.candidates
    }

    @Synchronized
    fun put(key: String, candidates: List<CandidateTrack>) {
        if (candidates.isEmpty()) return
        cache[key] = Entry(now(), candidates)
    }

    @Synchronized
    fun evict(key: String) {
        cache.remove(key)
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    companion object {
        private const val DEFAULT_TTL_MILLIS = 24L * 60 * 60 * 1000
    }
}