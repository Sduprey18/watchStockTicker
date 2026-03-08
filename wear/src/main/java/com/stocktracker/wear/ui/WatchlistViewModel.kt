package com.stocktracker.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocktracker.wear.data.ConnectivityObserver
import com.stocktracker.wear.data.SecureApiKeyManager
import com.stocktracker.wear.data.StockRepository
import com.stocktracker.wear.domain.StockQuote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class WatchlistUiState(
    val quotes: List<StockQuote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKeyConfigured: Boolean = true,
    val isOffline: Boolean = false,
    val lastSyncTime: Long? = null
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: StockRepository,
    private val secureApiKeyManager: SecureApiKeyManager,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistUiState(apiKeyConfigured = secureApiKeyManager.isKeyConfigured()))
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    init {
        loadWatchlistWithQuotes()
        observeConnectivity()
    }

    fun loadWatchlistWithQuotes() {
        viewModelScope.launch {
            repository.watchlistWithQuotesFlow().collect { quotes ->
                val latestSync = quotes.maxOfOrNull { it.fetchedAt }
                _state.update { it.copy(quotes = quotes, error = null, lastSyncTime = latestSync) }
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { connected ->
                _state.update { it.copy(isOffline = !connected) }
            }
        }
    }

    fun refresh() {
        if (!secureApiKeyManager.isKeyConfigured()) {
            Timber.w("Refresh requested but API key not configured")
            _state.update { it.copy(error = "Configure API key in local.properties") }
            return
        }
        Timber.d("Manual refresh triggered")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.refreshQuotes(force = true)
                .onFailure { e ->
                    Timber.w(e, "Refresh failed")
                    _state.update { it.copy(error = e.message ?: "Refresh failed") }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun addSymbol(symbol: String) {
        Timber.d("Adding symbol: %s", symbol)
        viewModelScope.launch {
            repository.addToWatchlist(symbol)
            if (secureApiKeyManager.isKeyConfigured()) {
                repository.refreshQuotes(force = true)
            }
        }
    }

    fun removeSymbol(symbol: String) {
        Timber.d("Removing symbol: %s", symbol)
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}


