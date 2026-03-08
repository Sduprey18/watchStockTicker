package com.stocktracker.wear.data

import com.stocktracker.wear.data.local.dao.QuoteDao
import com.stocktracker.wear.data.local.dao.WatchlistDao
import com.stocktracker.wear.data.local.entity.CachedQuoteEntity
import com.stocktracker.wear.data.local.entity.WatchlistEntity
import com.stocktracker.wear.data.remote.AlphaVantageApi
import com.stocktracker.wear.data.remote.RequestQueue
import com.stocktracker.wear.data.remote.dto.GlobalQuoteDto
import com.stocktracker.wear.data.remote.dto.GlobalQuoteInnerDto
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StockRepositoryTest {

    private lateinit var watchlistDao: WatchlistDao
    private lateinit var quoteDao: QuoteDao
    private lateinit var api: AlphaVantageApi
    private lateinit var secureApiKeyManager: SecureApiKeyManager
    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: StockRepository

    @Before
    fun setUp() {
        watchlistDao = mockk(relaxed = true)
        quoteDao = mockk(relaxed = true)
        api = mockk()
        secureApiKeyManager = mockk()
        // Use a real RequestQueue — it just serializes calls
        requestQueue = RequestQueue()

        every { secureApiKeyManager.getApiKey() } returns "test-api-key"

        repository = StockRepository(
            watchlistDao = watchlistDao,
            quoteDao = quoteDao,
            api = api,
            secureApiKeyManager = secureApiKeyManager,
            requestQueue = requestQueue
        )
    }

    // ── addToWatchlist ──────────────────────────────────────────

    @Test
    fun `addToWatchlist normalizes symbol to uppercase`() = runTest {
        coEvery { watchlistDao.getWatchlist() } returns emptyList()

        repository.addToWatchlist("aapl")

        coVerify {
            watchlistDao.insert(withArg { entity ->
                assertEquals("AAPL", entity.symbol)
            })
        }
    }

    @Test
    fun `addToWatchlist trims whitespace`() = runTest {
        coEvery { watchlistDao.getWatchlist() } returns emptyList()

        repository.addToWatchlist("  goog  ")

        coVerify {
            watchlistDao.insert(withArg { entity ->
                assertEquals("GOOG", entity.symbol)
            })
        }
    }

    @Test
    fun `addToWatchlist skips empty string`() = runTest {
        repository.addToWatchlist("")

        coVerify(exactly = 0) { watchlistDao.insert(any()) }
    }

    @Test
    fun `addToWatchlist skips whitespace-only string`() = runTest {
        repository.addToWatchlist("   ")

        coVerify(exactly = 0) { watchlistDao.insert(any()) }
    }

    @Test
    fun `addToWatchlist skips duplicate symbol`() = runTest {
        coEvery { watchlistDao.getWatchlist() } returns listOf(
            WatchlistEntity("AAPL", 0)
        )

        repository.addToWatchlist("AAPL")

        coVerify(exactly = 0) { watchlistDao.insert(any()) }
    }

    @Test
    fun `addToWatchlist assigns correct sortOrder`() = runTest {
        coEvery { watchlistDao.getWatchlist() } returns listOf(
            WatchlistEntity("AAPL", 0),
            WatchlistEntity("GOOG", 1)
        )

        repository.addToWatchlist("MSFT")

        coVerify {
            watchlistDao.insert(withArg { entity ->
                assertEquals("MSFT", entity.symbol)
                assertEquals(2, entity.sortOrder)
            })
        }
    }

    // ── removeFromWatchlist ─────────────────────────────────────

    @Test
    fun `removeFromWatchlist delegates to DAO`() = runTest {
        repository.removeFromWatchlist("AAPL")

        coVerify { watchlistDao.remove("AAPL") }
    }

    // ── refreshQuotes ───────────────────────────────────────────

    @Test
    fun `refreshQuotes fails when API key is blank`() = runTest {
        every { secureApiKeyManager.getApiKey() } returns ""

        val result = repository.refreshQuotes()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `refreshQuotes succeeds on empty watchlist`() = runTest {
        coEvery { watchlistDao.getWatchlist() } returns emptyList()

        val result = repository.refreshQuotes()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { api.getGlobalQuote(any(), any(), any()) }
    }

    @Test
    fun `refreshQuotes calls API for symbols without cache`() = runTest {
        coEvery { watchlistDao.getWatchlist() } returns listOf(
            WatchlistEntity("AAPL", 0)
        )
        coEvery { quoteDao.getQuote("AAPL") } returns null
        coEvery { api.getGlobalQuote(any(), symbol = "AAPL", apikey = "test-api-key") } returns
            GlobalQuoteDto(
                GlobalQuoteInnerDto(
                    symbol = "AAPL",
                    price = "150.00",
                    change = "2.00",
                    changePercent = "1.35%"
                )
            )

        val result = repository.refreshQuotes()

        assertTrue(result.isSuccess)
        coVerify { api.getGlobalQuote(any(), symbol = "AAPL", apikey = "test-api-key") }
        coVerify { quoteDao.insertAll(any()) }
    }

    @Test
    fun `refreshQuotes skips symbol with fresh cache`() = runTest {
        val freshEntity = CachedQuoteEntity(
            symbol = "AAPL",
            price = 150.0,
            change = 2.0,
            changePercent = 1.35,
            open = null,
            high = null,
            low = null,
            previousClose = null,
            latestTradingDay = null,
            fetchedAt = System.currentTimeMillis() // fresh
        )

        coEvery { watchlistDao.getWatchlist() } returns listOf(
            WatchlistEntity("AAPL", 0)
        )
        coEvery { quoteDao.getQuote("AAPL") } returns freshEntity

        val result = repository.refreshQuotes(force = false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { api.getGlobalQuote(any(), any(), any()) }
    }

    @Test
    fun `refreshQuotes force bypasses cache`() = runTest {
        val freshEntity = CachedQuoteEntity(
            symbol = "AAPL",
            price = 150.0,
            change = 2.0,
            changePercent = 1.35,
            open = null,
            high = null,
            low = null,
            previousClose = null,
            latestTradingDay = null,
            fetchedAt = System.currentTimeMillis()
        )

        coEvery { watchlistDao.getWatchlist() } returns listOf(
            WatchlistEntity("AAPL", 0)
        )
        coEvery { quoteDao.getQuote("AAPL") } returns freshEntity
        coEvery { api.getGlobalQuote(any(), symbol = "AAPL", apikey = "test-api-key") } returns
            GlobalQuoteDto(
                GlobalQuoteInnerDto(
                    symbol = "AAPL",
                    price = "155.00",
                    change = "5.00",
                    changePercent = "3.33%"
                )
            )

        val result = repository.refreshQuotes(force = true)

        assertTrue(result.isSuccess)
        coVerify { api.getGlobalQuote(any(), symbol = "AAPL", apikey = "test-api-key") }
    }

    @Test
    fun `refreshQuotes calls API for stale cache`() = runTest {
        val staleEntity = CachedQuoteEntity(
            symbol = "AAPL",
            price = 150.0,
            change = 2.0,
            changePercent = 1.35,
            open = null,
            high = null,
            low = null,
            previousClose = null,
            latestTradingDay = null,
            fetchedAt = System.currentTimeMillis() - (20 * 60 * 1000L) // 20 min ago → stale
        )

        coEvery { watchlistDao.getWatchlist() } returns listOf(
            WatchlistEntity("AAPL", 0)
        )
        coEvery { quoteDao.getQuote("AAPL") } returns staleEntity
        coEvery { api.getGlobalQuote(any(), symbol = "AAPL", apikey = "test-api-key") } returns
            GlobalQuoteDto(
                GlobalQuoteInnerDto(
                    symbol = "AAPL",
                    price = "152.00",
                    change = "2.00",
                    changePercent = "1.33%"
                )
            )

        val result = repository.refreshQuotes(force = false)

        assertTrue(result.isSuccess)
        coVerify { api.getGlobalQuote(any(), symbol = "AAPL", apikey = "test-api-key") }
    }

    // ── clearStaleCache ─────────────────────────────────────────

    @Test
    fun `clearStaleCache deletes entries older than 2x TTL`() = runTest {
        repository.clearStaleCache()

        coVerify { quoteDao.deleteOlderThan(any()) }
    }
}
