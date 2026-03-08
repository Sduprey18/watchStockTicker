package com.stocktracker.wear.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that handles HTTP 429 (Too Many Requests) responses
 * with exponential backoff. Reads the Retry-After header when present.
 */
@Singleton
class RateLimitInterceptor @Inject constructor() : Interceptor {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 16000L
        private const val BACKOFF_MULTIPLIER = 2.0
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0

        while (response.code == 429 && retryCount < MAX_RETRIES) {
            response.close()
            retryCount++

            val retryAfterHeader = response.header("Retry-After")
            val retryAfterMs = retryAfterHeader?.toLongOrNull()?.times(1000)

            val exponentialDelay = (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, (retryCount - 1).toDouble())).toLong()
                .coerceAtMost(MAX_DELAY_MS)

            val delayMs = if (retryAfterMs != null && retryAfterMs > exponentialDelay) {
                retryAfterMs.coerceAtMost(MAX_DELAY_MS)
            } else {
                exponentialDelay
            }

            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Request interrupted during rate-limit backoff")
            }

            response = chain.proceed(request)
        }

        return response
    }
}
