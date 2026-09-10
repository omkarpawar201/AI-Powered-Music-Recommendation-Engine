package com.musicengine.mediapoc.network

import com.musicengine.mediapoc.model.CandidateSource
import com.musicengine.mediapoc.model.CandidateTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogCacheTest {

    private var clock = 0L
    private val cache = CatalogCache(maxSize = 3, ttlMillis = 1000, now = { clock })

    private fun candidates(count: Int) = List(count) { i ->
        CandidateTrack(title = "Track $i", artist = "Artist", source = CandidateSource.CATALOG_SEARCH)
    }

    @Test
    fun `returns cached value within ttl`() {
        val value = candidates(2)
        cache.put("a", value)
        assertEquals(value, cache.get("a"))
    }

    @Test
    fun `expired entries are evicted`() {
        cache.put("a", candidates(1))
        clock += 1001
        assertNull(cache.get("a"))
    }

    @Test
    fun `empty results are never cached`() {
        cache.put("a", emptyList())
        assertNull(cache.get("a"))
    }

    @Test
    fun `evict removes an entry`() {
        cache.put("a", candidates(1))
        cache.evict("a")
        assertNull(cache.get("a"))
    }

    @Test
    fun `lru evicts oldest beyond maxSize`() {
        cache.put("a", candidates(1))
        cache.put("b", candidates(1))
        cache.put("c", candidates(1))
        val value = candidates(1)
        cache.put("d", value) // evicts least-recently-used entry "a"

        assertNull(cache.get("a"))
        assertEquals(value, cache.get("d"))
        assertEquals(candidates(1).size, cache.get("b")?.size)
    }
}