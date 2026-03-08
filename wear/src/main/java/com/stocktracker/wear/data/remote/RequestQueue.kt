package com.stocktracker.wear.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coroutine-based request queue that serializes API calls and enforces
 * a minimum inter-request delay to respect Alpha Vantage rate limits
 * (~5 requests per minute on the free tier).
 */
@Singleton
class RequestQueue @Inject constructor() {

    companion object {
        /** Minimum delay between consecutive API requests in milliseconds. */
        const val INTER_REQUEST_DELAY_MS = 1200L
    }

    private val mutex = Mutex()
    private var lastRequestTimeMs = 0L

    /**
     * Enqueues a suspending block for sequential execution, ensuring that
     * at least [INTER_REQUEST_DELAY_MS] has elapsed since the previous request.
     */
    suspend fun <T> enqueue(block: suspend () -> T): T = mutex.withLock {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTimeMs
        if (elapsed < INTER_REQUEST_DELAY_MS && lastRequestTimeMs > 0) {
            val waitMs = INTER_REQUEST_DELAY_MS - elapsed
            Timber.d("RequestQueue: throttling for %dms", waitMs)
            delay(waitMs)
        }
        try {
            block()
        } finally {
            lastRequestTimeMs = System.currentTimeMillis()
        }
    }
}

