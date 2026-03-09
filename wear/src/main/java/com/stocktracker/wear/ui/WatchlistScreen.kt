package com.stocktracker.wear.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.stocktracker.wear.domain.StockQuote
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchlistScreen(
    onSymbolClick: (String) -> Unit,
    onAddClick: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isAmbient = LocalAmbientMode.current

    if (isAmbient) {
        AmbientWatchlistContent(quotes = state.quotes, lastSyncTime = state.lastSyncTime)
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.error != null -> ErrorContent(
                message = state.error!!,
                onDismiss = viewModel::clearError
            )
            state.quotes.isEmpty() -> EmptyWatchlist(
                onAddClick = onAddClick,
                apiKeyConfigured = state.apiKeyConfigured
            )
            else -> {
                val listState = rememberScalingLazyListState()
                val focusRequester = remember { FocusRequester() }
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .onRotaryScrollEvent { event ->
                            coroutineScope.launch {
                                listState.scrollBy(event.verticalScrollPixels)
                            }
                            true
                        }
                        .focusRequester(focusRequester)
                        .focusable(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Offline banner
                    if (state.isOffline) {
                        item {
                            Text(
                                text = "⚡ Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.refresh() },
                            enabled = !state.isLoading
                        ) {
                            Text(if (state.isLoading) "Refreshing…" else "Refresh")
                        }
                    }
                    items(state.quotes) { quote ->
                        StockQuoteCard(
                            quote = quote,
                            onClick = { onSymbolClick(quote.symbol) }
                        )
                    }
                    item {
                        Button(onClick = onAddClick) {
                            Text("Add symbol")
                        }
                    }

                    // Last sync timestamp
                    state.lastSyncTime?.let { ts ->
                        item {
                            Text(
                                text = "Last sync: ${formatFetchedAt(ts)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

/**
 * Simplified ambient mode layout: white-on-black, no interactive elements,
 * shows top 3 symbols with prices for at-a-glance readability.
 */
@Composable
private fun AmbientWatchlistContent(
    quotes: List<StockQuote>,
    lastSyncTime: Long?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        quotes.take(3).forEach { quote ->
            val changeStr = "%+.2f".format(quote.change)
            Text(
                text = "${quote.symbol}  ${"%.2f".format(quote.price)}  $changeStr",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        if (quotes.isEmpty()) {
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
        lastSyncTime?.let {
            Text(
                text = formatFetchedAt(it),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StockQuoteCard(
    quote: StockQuote,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = quote.symbol,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "%.2f".format(quote.price),
                style = MaterialTheme.typography.bodyLarge
            )
            val changeStr = "%+.2f (%+.2f%%)".format(quote.change, quote.changePercent)
            val color = if (quote.change >= 0)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.error
            Text(
                text = changeStr,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyWatchlist(
    onAddClick: () -> Unit,
    apiKeyConfigured: Boolean
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = if (apiKeyConfigured) "No symbols" else "Configure API key",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        item {
            Text(
                text = if (apiKeyConfigured) "Add symbols to track" else "Add STOCK_API_KEY to local.properties",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        if (apiKeyConfigured) {
            item {
                Button(onClick = onAddClick, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Add symbol")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onDismiss: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        item {
            Button(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                Text("OK")
            }
        }
    }
}

fun formatFetchedAt(ts: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
}
