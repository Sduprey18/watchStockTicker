package com.stocktracker.wear.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocktracker.wear.data.ConnectivityObserver
import com.stocktracker.wear.domain.StockQuote
import com.stocktracker.wear.data.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val quote: StockQuote? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: StockRepository,
    private val connectivityObserver: ConnectivityObserver,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val symbol: String = checkNotNull(savedStateHandle["symbol"]) { "symbol required" }

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getQuoteFlow(symbol).collect { quote ->
                _state.update { it.copy(quote = quote) }
            }
        }
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { connected ->
                _state.update { it.copy(isOffline = !connected) }
            }
        }
    }

    fun removeFromWatchlist() {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }
}

