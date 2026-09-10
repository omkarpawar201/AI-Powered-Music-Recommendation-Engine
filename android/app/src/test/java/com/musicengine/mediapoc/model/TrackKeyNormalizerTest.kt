package com.musicengine.mediapoc.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackKeyNormalizerTest {

    @Test
    fun `feat parenthetical is stripped`() {
        assertEquals(
            "levitating - dua lipa",
            TrackKeyNormalizer.canonicalKey("Levitating (feat. DaBaby)", "Dua Lipa")
        )
    }

    @Test
    fun `feat in artist position is stripped`() {
        assertEquals(
            "death bed - powfu",
            TrackKeyNormalizer.canonicalKey("death bed", "Powfu (feat. beabadoobee)")
        )
    }

    @Test
    fun `bracket feat variant is stripped`() {
        assertEquals(
            "rockstar - post malone",
            TrackKeyNormalizer.canonicalKey("rockstar [feat. 21 Savage]", "Post Malone")
        )
    }

    @Test
    fun `soundtrack from tag is stripped`() {
        assertEquals(
            "ilahi - arijit singh",
            TrackKeyNormalizer.canonicalKey("Ilahi (From \"Yeh Jawaani Hai Deewani\")", "Arijit Singh")
        )
    }

    @Test
    fun `remastered edition suffix is stripped`() {
        assertEquals(
            "hotel california - eagles",
            TrackKeyNormalizer.canonicalKey("Hotel California - 2013 Remaster", "Eagles")
        )
    }

    @Test
    fun `explicit marker is stripped`() {
        assertEquals(
            "godzilla - eminem",
            TrackKeyNormalizer.canonicalKey("Godzilla [Explicit]", "Eminem")
        )
    }

    @Test
    fun `smart quotes and unicode normalize to plain`() {
        assertEquals(
            "don't stop believin' - journey",
            TrackKeyNormalizer.canonicalKey("Don\u2019t Stop Believin\u2019", "Journey")
        )
    }

    @Test
    fun `case and whitespace are normalized`() {
        assertEquals(
            "thriller - michael jackson",
            TrackKeyNormalizer.canonicalKey("   Thriller   ", " MICHAEL JACKSON ")
        )
    }

    @Test
    fun `plain parentheticals are kept`() {
        // Conservative: live/studio variants are distinct listens, not release tags
        assertEquals(
            "abrakadabra (live) - smino",
            TrackKeyNormalizer.canonicalKey("Abrakadabra (Live)", "Smino")
        )
    }

    @Test
    fun `canonicalKeyFromStoredKey round-trips`() {
        val stored = TrackKeyNormalizer.canonicalKey("Hotel California - 2013 Remaster", "Eagles")
        assertEquals("hotel california - eagles", TrackKeyNormalizer.canonicalKeyFromStoredKey(stored))
    }

    @Test
    fun `canonicalKeyFromStoredKey rejects unparseable keys`() {
        assertNull(TrackKeyNormalizer.canonicalKeyFromStoredKey("no-separator-here"))
        assertNull(TrackKeyNormalizer.canonicalKeyFromStoredKey(""))
        assertNull(TrackKeyNormalizer.canonicalKeyFromStoredKey("onlyartist -"))
    }

    @Test
    fun `blank fields produce blank key`() {
        assertEquals("", TrackKeyNormalizer.canonicalKey("", "Artist"))
        assertEquals("", TrackKeyNormalizer.canonicalKey("Title", ""))
    }
}