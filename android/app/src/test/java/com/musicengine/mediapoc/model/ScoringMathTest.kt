package com.musicengine.mediapoc.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringMathTest {

    // ─── transitionScore (Laplace-smoothed Markov) ─────────────────────────

    @Test
    fun `transitionScore_all successes yields max 1`() {
        val score = ScoringMath.transitionScore(successCount = 8, earlySkipCount = 0, lateSkipCount = 0, totalTransitions = 8)
        assertEquals(0.8f, score, 0.0001f)
        assertTrue(score in -1.0f..1.0f)
    }

    @Test
    fun `transitionScore_earlySkips penalty factor`() {
        // One success kept positive; early skips weigh 1.5 each
        val score = ScoringMath.transitionScore(successCount = 1, earlySkipCount = 2, lateSkipCount = 0, totalTransitions = 3)
        val expected = (1.0 - 1.5 * 2.0) / (3.0 + 2.0) // -2.0 / 5.0 = -0.4
        assertEquals(expected.toFloat(), score, 0.0001f)
    }

    @Test
    fun `transitionScore_lateSkips penalty lighter than early`() {
        val early = ScoringMath.transitionScore(successCount = 0, earlySkipCount = 2, lateSkipCount = 0, totalTransitions = 2)
        val late = ScoringMath.transitionScore(successCount = 0, earlySkipCount = 0, lateSkipCount = 2, totalTransitions = 2)
        assertTrue(late > early) // -0.25 vs -0.6
    }

    @Test
    fun `transitionScore_is clamped to lower bound`() {
        val score = ScoringMath.transitionScore(successCount = 0, earlySkipCount = 10, lateSkipCount = 0, totalTransitions = 10)
        assertEquals(-1.0f, score, 0.0001f)
    }

    // ─── effectivePenalty (exponential half-life decay) ────────────────────

    @Test
    fun `effectivePenalty_fresh penalty equals initial`() {
        val now = System.currentTimeMillis()
        val effective = ScoringMath.effectivePenalty(initialPenalty = 40f, skipTimestamp = now, halfLifeHours = 4f, currentTimestamp = now)
        assertEquals(40f, effective, 0.0001f)
    }

    @Test
    fun `effectivePenalty_halfWay through half-life halves penalty`() {
        val skipAt = 0L
        val halfLifeHours = 4f
        val elapsedAtHalfLifeMs = (halfLifeHours.toLong() * 3600L) * 1000L
        val effective = ScoringMath.effectivePenalty(initialPenalty = 40f, skipTimestamp = skipAt, halfLifeHours = halfLifeHours, currentTimestamp = elapsedAtHalfLifeMs)
        assertEquals(20f, effective, 0.5f)
    }

    @Test
    fun `effectivePenalty_negative elapsed coerces to zero delta`() {
        val now = System.currentTimeMillis()
        val effective = ScoringMath.effectivePenalty(40f, skipTimestamp = now + 60_000L, halfLifeHours = 4f, currentTimestamp = now)
        assertEquals(40f, effective, 0.0001f)
    }

    // ─── combinedScore (floor at zero) ─────────────────────────────────────

    @Test
    fun `combinedScore primary factors add up`() {
        val score = ScoringMath.combinedScore(
            longTermPref = 18f, transitionPoints = 12f, artistAffinity = 12f, genreAffinity = 8f,
            replayBoost = 6f, noveltyBoost = 0f, skipPenalty = 0f, recencyPenalty = 0f
        )
        assertEquals(56f, score, 0.0001f)
    }

    @Test
    fun `combinedScore floors at zero when penalties dominate`() {
        val score = ScoringMath.combinedScore(
            longTermPref = 8f, transitionPoints = 0f, artistAffinity = 4f, genreAffinity = 0f,
            replayBoost = 0f, noveltyBoost = 5f, skipPenalty = 40f, recencyPenalty = 30f
        )
        assertEquals(0f, score, 0.0001f)
    }
}