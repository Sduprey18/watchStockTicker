package com.stocktracker.wear.data

import com.stocktracker.wear.BuildConfig
import com.stocktracker.wear.data.local.CachedQuoteEntityMapper.toDomain
import com.stocktracker.wear.data.local.CachedQuoteEntityMapper.toEntity
import com.stocktracker.wear.data.local.dao.QuoteDao
import com.stocktracker.wear.data.local.dao.WatchlistDao
import com.stocktracker.wear.data.local.entity.WatchlistEntity
import com.stocktracker.wear.data.remote.AlphaVantageApi
import com.stocktracker.wear.data.remote.toDomain
import com.stocktracker.wear.domain.StockQuote
import com.stocktracker.wear.domain.WatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes

@Singleton
class StockRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val quoteDao: QuoteDao,
    private val api: AlphaVantageApi
) {

    private val apiKey: String
        get() = BuildConfig.STOCK_API_KEY

    private val refreshLocks = ConcurrentHashMap<String, Any>()
    private val lock = Any()

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
        if (existing.any { it.symbol == normalized }) return
        watchlistDao.insert(WatchlistEntity(normalized, existing.size))
    }

    suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.remove(symbol)
    }

    suspend fun refreshQuotes(force: Boolean = false): Result<Unit> {
        if (apiKey.isBlank()) return Result.failure(SecurityException("Configure API key"))
        val symbols = watchlistDao.getWatchlist().map { it.symbol }
        if (symbols.isEmpty()) return Result.success(Unit)
        val results = mutableListOf<StockQuote>()
        for (sym in symbols) {
            val cached = quoteDao.getQuote(sym)
            if (!force && cached != null && (System.currentTimeMillis() - cached.fetchedAt) < CACHE_TTL_MS) {
                results.add(cached.toDomain())
                continue
            }
            val lockObj = refreshLocks.getOrPut(sym) { Any() }
            synchronized(lockObj) {
                val dto = runCatching { api.getGlobalQuote(symbol = sym, apikey = apiKey) }
                dto.getOrNull()?.toDomain(sym)?.let { results.add(it) }
            }
        }
        if (results.isNotEmpty()) {
            quoteDao.insertAll(results.map { it.toEntity() })
        }
        return Result.success(Unit)
    }

    fun getQuoteFlow(symbol: String): Flow<StockQuote?> =
        quoteDao.quoteFlow(symbol).map { it?.toDomain() }

    suspend fun getQuote(symbol: String): StockQuote? =
        quoteDao.getQuote(symbol)?.toDomain()

    suspend fun clearStaleCache() {
        quoteDao.deleteOlderThan(System.currentTimeMillis() - CACHE_TTL_MS * 2)
    }
}
