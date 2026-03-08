package com.stocktracker.wear.ui

import androidx.lifecycle.SavedStateHandle
import com.stocktracker.wear.data.ConnectivityObserver
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
class DetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: StockRepository
    private lateinit var connectivityObserver: ConnectivityObserver
    private lateinit var connectivityFlow: MutableStateFlow<Boolean>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        connectivityObserver = mockk()
        connectivityFlow = MutableStateFlow(true)

        every { connectivityObserver.isConnected } returns connectivityFlow
        every { repository.getQuoteFlow(any()) } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(symbol: String = "AAPL"): DetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("symbol" to symbol))
        return DetailViewModel(
            repository = repository,
            connectivityObserver = connectivityObserver,
            savedStateHandle = savedStateHandle
        )
    }

    // ── Symbol from SavedStateHandle ────────────────────────────

    @Test
    fun `symbol is read from SavedStateHandle`() {
        val viewModel = createViewModel("TSLA")

        assertEquals("TSLA", viewModel.symbol)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when symbol is missing from SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle(emptyMap())
        DetailViewModel(
            repository = repository,
            connectivityObserver = connectivityObserver,
            savedStateHandle = savedStateHandle
        )
    }

    // ── Quote Flow ──────────────────────────────────────────────

    @Test
    fun `state updates when quote flow emits`() = runTest {
        val quote = StockQuote("AAPL", 150.0, 2.0, 1.35, fetchedAt = 1000L)
        every { repository.getQuoteFlow("AAPL") } returns flowOf(quote)

        val viewModel = createViewModel("AAPL")

        assertNotNull(viewModel.state.value.quote)
        assertEquals("AAPL", viewModel.state.value.quote!!.symbol)
        assertEquals(150.0, viewModel.state.value.quote!!.price, 0.001)
    }

    @Test
    fun `state has null quote when flow emits null`() = runTest {
        every { repository.getQuoteFlow("AAPL") } returns flowOf(null)

        val viewModel = createViewModel("AAPL")

        assertNull(viewModel.state.value.quote)
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

    // ── removeFromWatchlist ─────────────────────────────────────

    @Test
    fun `removeFromWatchlist delegates to repository`() = runTest {
        val viewModel = createViewModel("GOOG")
        viewModel.removeFromWatchlist()

        coVerify { repository.removeFromWatchlist("GOOG") }
    }

    @Test
    fun `removeFromWatchlist uses correct symbol from SavedStateHandle`() = runTest {
        val viewModel = createViewModel("MSFT")
        viewModel.removeFromWatchlist()

        coVerify { repository.removeFromWatchlist("MSFT") }
    }
}
