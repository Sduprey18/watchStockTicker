package com.stocktracker.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocktracker.wear.BuildConfig
import com.stocktracker.wear.data.StockRepository
import com.stocktracker.wear.domain.StockQuote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val quotes: List<StockQuote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKeyConfigured: Boolean = true
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistUiState(apiKeyConfigured = BuildConfig.STOCK_API_KEY.isNotBlank()))
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    init {
        loadWatchlistWithQuotes()
    }

    fun loadWatchlistWithQuotes() {
        viewModelScope.launch {
            repository.watchlistWithQuotesFlow().collect { quotes ->
                _state.update { it.copy(quotes = quotes, error = null) }
            }
        }
    }

    fun refresh() {
        if (BuildConfig.STOCK_API_KEY.isBlank()) {
            _state.update { it.copy(error = "Configure API key in local.properties") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.refreshQuotes(force = true)
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Refresh failed") } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun addSymbol(symbol: String) {
        viewModelScope.launch {
            repository.addToWatchlist(symbol)
            if (BuildConfig.STOCK_API_KEY.isNotBlank()) {
                repository.refreshQuotes(force = true)
            }
        }
    }

    fun removeSymbol(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
