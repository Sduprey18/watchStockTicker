package com.stocktracker.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.stocktracker.wear.domain.StockQuote
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
            else -> ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally)
            {
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
            }
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
