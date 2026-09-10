package com.musicengine.mediapoc.model

import java.text.Normalizer
import java.util.Locale

/**
 * Produces canonical track keys so that the same song reported by different
 * music apps (Spotify, Apple Music, YT Music, iTunes) collapses to one identity.
 *
 * Rules are deliberately conservative:
 *  - Unicode NFC normalization + smart/curly quote & dash mapping
 *  - Lowercase, trimmed, collapsed whitespace
 *  - Strips common release tags: (feat. X), (From "Movie"), [Explicit], (2013 Remaster)
 */
object TrackKeyNormalizer {

    /** Canonical key: "title - artist", both passed through [normalizeField]. */
    fun canonicalKey(title: String, artist: String): String {
        val t = normalizeField(title)
        val a = normalizeField(artist)
        if (t.isBlank() || a.isBlank()) return ""
        return "$t - $a"
    }

    /**
     * Parses a previously-stored "title - artist" key back into its canonical form.
     * Returns null when the key has no separator (unparseable).
     */
    fun canonicalKeyFromStoredKey(storedKey: String): String? {
        if (storedKey.isBlank()) return null
        val separator = storedKey.lastIndexOf(" - ")
        if (separator <= 0) return null
        val title = storedKey.substring(0, separator)
        val artist = storedKey.substring(separator + 3)
        if (title.isBlank() || artist.isBlank()) return null
        return canonicalKey(title, artist)
    }

    fun normalizeField(input: String): String {
        if (input.isEmpty()) return input
        var s = Normalizer.normalize(input, Normalizer.Form.NFC)

        // Map typographic characters to plain ASCII equivalents
        s = s
            .replace('\u2018', '\'')  // ' left single quote
            .replace('\u2019', '\'')  // ' right single quote
            .replace('\u201a', '\'')  // ' single low quote
            .replace('\u201c', '"')   // " left double quote
            .replace('\u201d', '"')   // " right double quote
            .replace('\u201e', '"')   // " double low quote
            .replace('\u2013', '-')   // en dash
            .replace('\u2014', '-')   // em dash
            .replace('\u2212', '-')   // minus sign
            .replace('\u00a0', ' ')   // non-breaking space

        // Strip common release/version tags
        s = stripTags(s)

        // Collapse whitespace, trim, lowercase for stable matching
        s = s.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT)
        return s
    }

    private fun stripTags(input: String): String {
        var s = input
        for (pattern in TAG_PATTERNS) {
            s = pattern.replace(s, "").trim()
        }
        return s
    }

    private val TAG_PATTERNS = listOf(
        // (feat. X), (ft. X), (featuring X)  -- also bracket variant
        Regex("""\((?:feat\.?|ft\.?|featuring)\s+[^)]*\)""", RegexOption.IGNORE_CASE),
        Regex("""\[(?:feat\.?|ft\.?|featuring)\s+[^\]]*\]""", RegexOption.IGNORE_CASE),
        // (From "Movie"), (From The Motion Picture "..."), (OST ...)
        Regex("""\((?:from|ost|theme)\b[^)]*\)""", RegexOption.IGNORE_CASE),
        // [Explicit], [Clean]
        Regex("""\[(?:explicit|clean)\]""", RegexOption.IGNORE_CASE),
        // (2013 Remaster), (Remastered), (Deluxe Edition)
        Regex("""\((?:remaster(?:ed)?|\d{4}\s+remaster(?:ed)?|deluxe\s+edition|dlx\.?\s*edition)\)""", RegexOption.IGNORE_CASE),
        Regex("""\[(?:remaster(?:ed)?|\d{4}\s+remaster(?:ed)?|deluxe\s+edition|dlx\.?\s*edition)\]""", RegexOption.IGNORE_CASE),
        // Trailing "- 2013 Remaster"/"- Remastered"
        Regex("""-\s*(?:\d{4}\s+)?remaster(?:ed)?\b.*$""", RegexOption.IGNORE_CASE)
    )
}