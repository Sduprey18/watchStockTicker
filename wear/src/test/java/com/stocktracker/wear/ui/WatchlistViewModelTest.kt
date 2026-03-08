package com.stocktracker.wear.ui

import com.stocktracker.wear.data.ConnectivityObserver
import com.stocktracker.wear.data.SecureApiKeyManager
import com.stocktracker.wear.data.StockRepository
import com.stocktracker.wear.domain.StockQuote
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: StockRepository
    private lateinit var secureApiKeyManager: SecureApiKeyManager
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var connectivityFlow: MutableStateFlow<Boolean>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        secureApiKeyManager = mockk()
        connectivityObserver = mockk()
        connectivityFlow = MutableStateFlow(true)

        every { secureApiKeyManager.isKeyConfigured() } returns true
        every { connectivityObserver.isConnected } returns connectivityFlow
        every { repository.watchlistWithQuotesFlow() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = WatchlistViewModel(
        repository = repository,
        secureApiKeyManager = secureApiKeyManager,
        connectivityObserver = connectivityObserver
    )

    // ── Initial State ───────────────────────────────────────────

    @Test
    fun `initial state has apiKeyConfigured when key is present`() {
        val viewModel = createViewModel()

        assertTrue(viewModel.state.value.apiKeyConfigured)
    }

    @Test
    fun `initial state has apiKeyConfigured false when key missing`() {
        every { secureApiKeyManager.isKeyConfigured() } returns false

        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.apiKeyConfigured)
    }

    @Test
    fun `initial state shows online when connected`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.isOffline)
    }

    // ── refresh ─────────────────────────────────────────────────

    @Test
    fun `refresh sets error when API key not configured`() {
        every { secureApiKeyManager.isKeyConfigured() } returns false

        val viewModel = createViewModel()
        viewModel.refresh()

        assertNotNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.error!!.contains("API key"))
    }

    @Test
    fun `refresh calls repository when API key is configured`() = runTest {
        coEvery { repository.refreshQuotes(force = true) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.refresh()

        coVerify { repository.refreshQuotes(force = true) }
    }

    @Test
    fun `refresh sets error on repository failure`() = runTest {
        coEvery { repository.refreshQuotes(force = true) } returns
            Result.failure(RuntimeException("Network error"))

        val viewModel = createViewModel()
        viewModel.refresh()

        assertEquals("Network error", viewModel.state.value.error)
    }

    @Test
    fun `refresh clears loading state after completion`() = runTest {
        coEvery { repository.refreshQuotes(force = true) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.refresh()

        assertFalse(viewModel.state.value.isLoading)
    }

    // ── addSymbol ───────────────────────────────────────────────

    @Test
    fun `addSymbol delegates to repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.addSymbol("AAPL")

        coVerify { repository.addToWatchlist("AAPL") }
    }

    @Test
    fun `addSymbol triggers refresh when API key configured`() = runTest {
        coEvery { repository.refreshQuotes(force = true) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.addSymbol("AAPL")

        coVerify { repository.addToWatchlist("AAPL") }
        coVerify { repository.refreshQuotes(force = true) }
    }

    @Test
    fun `addSymbol does not refresh when API key not configured`() = runTest {
        every { secureApiKeyManager.isKeyConfigured() } returns false

        val viewModel = createViewModel()
        viewModel.addSymbol("AAPL")

        coVerify { repository.addToWatchlist("AAPL") }
        coVerify(exactly = 0) { repository.refreshQuotes(any()) }
    }

    // ── removeSymbol ────────────────────────────────────────────

    @Test
    fun `removeSymbol delegates to repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.removeSymbol("AAPL")

        coVerify { repository.removeFromWatchlist("AAPL") }
    }

    // ── clearError ──────────────────────────────────────────────

    @Test
    fun `clearError sets error to null`() {
        every { secureApiKeyManager.isKeyConfigured() } returns false

        val viewModel = createViewModel()
        viewModel.refresh() // sets an error
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    // ── Connectivity ────────────────────────────────────────────

    @Test
    fun `isOffline updates when connectivity changes`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.isOffline)

        connectivityFlow.value = false
        assertTrue(viewModel.state.value.isOffline)

        connectivityFlow.value = true
        assertFalse(viewModel.state.value.isOffline)
    }

    // ── Watchlist data ──────────────────────────────────────────

    @Test
    fun `state updates when watchlist flow emits`() = runTest {
        val quotes = listOf(
            StockQuote("AAPL", 150.0, 2.0, 1.35, fetchedAt = 1000L),
            StockQuote("GOOG", 2800.0, -10.0, -0.35, fetchedAt = 2000L)
        )
        every { repository.watchlistWithQuotesFlow() } returns flowOf(quotes)

        val viewModel = createViewModel()

        assertEquals(2, viewModel.state.value.quotes.size)
        assertEquals("AAPL", viewModel.state.value.quotes[0].symbol)
        assertEquals("GOOG", viewModel.state.value.quotes[1].symbol)
    }

    @Test
    fun `lastSyncTime is set from most recent fetchedAt`() = runTest {
        val quotes = listOf(
            StockQuote("AAPL", 150.0, 2.0, 1.35, fetchedAt = 1000L),
            StockQuote("GOOG", 2800.0, -10.0, -0.35, fetchedAt = 5000L)
        )
        every { repository.watchlistWithQuotesFlow() } returns flowOf(quotes)

        val viewModel = createViewModel()

        assertEquals(5000L, viewModel.state.value.lastSyncTime)
    }
}
