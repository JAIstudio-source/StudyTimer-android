package com.madeby.JAI

import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Lightweight network resilience utilities for retrying transient connection drops.
 */
object NetworkResilience {

    /**
     * Executes the given [block], retrying on transient [IOException] network failures
     * with exponential backoff. Non-network exceptions fail immediately.
     */
    suspend fun <T> retryWithBackoff(
        times: Int = 2,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 3000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (e !is IOException) throw e
                android.util.Log.d("NetworkResilience", "Transient network failure (attempt ${attempt + 1}), retrying in ${currentDelay}ms: ${e.message}")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
        return block()
    }
}
