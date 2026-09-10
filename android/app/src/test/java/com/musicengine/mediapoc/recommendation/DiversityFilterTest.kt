package com.musicengine.mediapoc.recommendation

import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.ScoreBreakdown
import com.musicengine.mediapoc.model.ScoredCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiversityFilterTest {

    private fun scored(title: String, artist: String) =
        ScoredCandidate(CandidateTrack(title = title, artist = artist), ScoreBreakdown())

    @Test
    fun `caps artists at maxPerArtist preserving order`() {
        val list = listOf(
            scored("a1", "Artist A"),
            scored("b1", "Artist B"),
            scored("a2", "Artist A"),
            scored("a3", "Artist A"),
            scored("c1", "Artist C")
        )
        val out = DiversityFilter.apply(list, maxPerArtist = 2)
        assertEquals(listOf("a1", "b1", "a2", "c1"), out.map { it.track.title })
    }

    @Test
    fun `zero max per artist disables filtering`() {
        val list = listOf(
            scored("a1", "Artist A"),
            scored("a2", "Artist A"),
            scored("a3", "Artist A")
        )
        assertEquals(list, DiversityFilter.apply(list, maxPerArtist = 0))
    }

    @Test
    fun `artist matching is case-insensitive`() {
        val list = listOf(
            scored("a1", "Artist A"),
            scored("b1", "artist a")
        )
        val out = DiversityFilter.apply(list, maxPerArtist = 1)
        assertEquals(listOf("a1"), out.map { it.track.title })
    }

    @Test
    fun `handles empty input`() {
        assertTrue(DiversityFilter.apply(emptyList(), maxPerArtist = 2).isEmpty())
    }

    @Test
    fun `keeps ranking order of survivors`() {
        val list = listOf(
            scored("first", "A"),
            scored("second", "B"),
            scored("third", "A"),
            scored("fourth", "C")
        )
        val out = DiversityFilter.apply(list, maxPerArtist = 1)
        assertEquals(listOf("first", "second", "fourth"), out.map { it.track.title })
    }
}