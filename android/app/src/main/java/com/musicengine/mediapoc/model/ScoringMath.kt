package com.musicengine.mediapoc.model

import kotlin.math.pow

/**
 * Pure, framework-free scoring math used by the recommendation engine.
 * Kept dependency-free so it can be unit tested on the JVM without Android/Room.
 */
object ScoringMath {

    /**
     * Exponential Half-Life Decay:
     * P(t) = P0 * (1/2) ^ (elapsedTime / halfLife)
     */
    fun effectivePenalty(
        initialPenalty: Float,
        skipTimestamp: Long,
        halfLifeHours: Float,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Float {
        val elapsedMs = (currentTimestamp - skipTimestamp).coerceAtLeast(0L)
        val elapsedHours = elapsedMs / (1000.0 * 3600.0)
        val halfLife = halfLifeHours.toDouble().coerceAtLeast(0.1)
        val decayFactor = (0.5).pow(elapsedHours / halfLife)
        return (initialPenalty * decayFactor).toFloat()
    }

    /**
     * Laplace-Smoothed Markov Transition Score:
     * S = (successes - 1.5 * earlySkips - 0.5 * lateSkips) / (totalTransitions + 2.0)
     * Normalized between -1.0 and +1.0.
     */
    fun transitionScore(
        successCount: Int,
        earlySkipCount: Int,
        lateSkipCount: Int,
        totalTransitions: Int
    ): Float {
        val rawNumerator = successCount.toDouble() - (1.5 * earlySkipCount) - (0.5 * lateSkipCount)
        val denominator = totalTransitions.toDouble() + 2.0 // Laplace smoothing (alpha = 2)
        return (rawNumerator / denominator).toFloat().coerceIn(-1.0f, 1.0f)
    }

    /**
     * Unified local ranking score, floored at zero.
     */
    fun combinedScore(
        longTermPref: Float,
        transitionPoints: Float,
        artistAffinity: Float,
        genreAffinity: Float,
        replayBoost: Float,
        noveltyBoost: Float,
        skipPenalty: Float,
        recencyPenalty: Float
    ): Float {
        val rawTotal = (longTermPref + transitionPoints + artistAffinity + genreAffinity + replayBoost + noveltyBoost) -
            skipPenalty - recencyPenalty
        return rawTotal.coerceAtLeast(0.0f)
    }
}