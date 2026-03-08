package com.stocktracker.wear.data

import com.stocktracker.wear.data.local.CachedQuoteEntityMapper.toDomain
import com.stocktracker.wear.data.local.CachedQuoteEntityMapper.toEntity
import com.stocktracker.wear.data.local.dao.QuoteDao
import com.stocktracker.wear.data.local.dao.WatchlistDao
import com.stocktracker.wear.data.local.entity.WatchlistEntity
import com.stocktracker.wear.data.remote.AlphaVantageApi
import com.stocktracker.wear.data.remote.RequestQueue
import com.stocktracker.wear.data.remote.toDomain
import com.stocktracker.wear.domain.StockQuote
import com.stocktracker.wear.domain.WatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes

@Singleton
class StockRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val quoteDao: QuoteDao,
    private val api: AlphaVantageApi,
    private val secureApiKeyManager: SecureApiKeyManager,
    private val requestQueue: RequestQueue
) {

    private val apiKey: String
        get() = secureApiKeyManager.getApiKey()

    fun watchlistFlow(): Flow<List<WatchlistItem>> =
        watchlistDao.watchlistFlow().map { list ->
            list.map { WatchlistItem(it.symbol, it.sortOrder) }
        }

    fun watchlistWithQuotesFlow(): Flow<List<StockQuote>> =
        combine(
            watchlistDao.watchlistFlow(),
            quoteDao.allQuotesFlow()
        ) { watchlist, quotes ->
            val quoteMap = quotes.associateBy { it.symbol }
            watchlist.mapNotNull { entity ->
                quoteMap[entity.symbol]?.toDomain()
            }
        }

    suspend fun addToWatchlist(symbol: String) {
        val normalized = symbol.uppercase().trim().ifEmpty { return }
        val existing = watchlistDao.getWatchlist()
        if (existing.any { it.symbol == normalized }) {
            Timber.d("Symbol %s already in watchlist, skipping", normalized)
            return
        }
        watchlistDao.insert(WatchlistEntity(normalized, existing.size))
        Timber.i("Added %s to watchlist (position %d)", normalized, existing.size)
    }

    suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.remove(symbol)
        Timber.i("Removed %s from watchlist", symbol)
    }

    suspend fun refreshQuotes(force: Boolean = false): Result<Unit> {
        if (apiKey.isBlank()) {
            Timber.w("refreshQuotes aborted: API key not configured")
            return Result.failure(SecurityException("Configure API key"))
        }
        val symbols = watchlistDao.getWatchlist().map { it.symbol }
        if (symbols.isEmpty()) {
            Timber.d("refreshQuotes: watchlist empty, nothing to refresh")
            return Result.success(Unit)
        }
        Timber.d("refreshQuotes started: %d symbols, force=%b", symbols.size, force)
        val results = mutableListOf<StockQuote>()
        for (sym in symbols) {
            val cached = quoteDao.getQuote(sym)
            if (!force && cached != null && (System.currentTimeMillis() - cached.fetchedAt) < CACHE_TTL_MS) {
                Timber.d("  %s: cache hit (age %ds)", sym, (System.currentTimeMillis() - cached.fetchedAt) / 1000)
                results.add(cached.toDomain())
                continue
            }
            val quote = requestQueue.enqueue {
                runCatching { api.getGlobalQuote(symbol = sym, apikey = apiKey) }
                    .onFailure { e -> Timber.w(e, "  %s: API call failed", sym) }
                    .getOrNull()?.toDomain(sym)
            }
            if (quote != null) {
                Timber.d("  %s: fetched successfully", sym)
                results.add(quote)
            } else {
                Timber.w("  %s: no quote returned", sym)
            }
        }
        if (results.isNotEmpty()) {
            quoteDao.insertAll(results.map { it.toEntity() })
        }
        Timber.d("refreshQuotes complete: %d/%d quotes updated", results.size, symbols.size)
        return Result.success(Unit)
    }

    fun getQuoteFlow(symbol: String): Flow<StockQuote?> =
        quoteDao.quoteFlow(symbol).map { it?.toDomain() }

    suspend fun getQuote(symbol: String): StockQuote? =
        quoteDao.getQuote(symbol)?.toDomain()

    suspend fun clearStaleCache() {
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MS * 2
        quoteDao.deleteOlderThan(cutoff)
        Timber.d("Cleared stale cache entries older than %d", cutoff)
    }
}

