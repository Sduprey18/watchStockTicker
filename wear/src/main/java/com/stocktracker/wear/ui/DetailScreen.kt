package com.stocktracker.wear.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.stocktracker.wear.domain.StockQuote
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isAmbient = LocalAmbientMode.current

    if (isAmbient) {
        AmbientDetailContent(quote = state.quote)
        return
    }

    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    listState.scrollBy(event.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Offline indicator
        if (state.isOffline) {
            item {
                Text(
                    text = "Offline — showing cached data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }

        state.quote?.let { quote ->
            item {
                Text(
                    text = quote.symbol,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                Text(
                    text = "%.2f".format(quote.price),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                val changeStr = "%+.2f (%+.2f%%)".format(quote.change, quote.changePercent)
                val color = if (quote.change >= 0)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.error
                Text(
                    text = changeStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
            quote.open?.let { open ->
                item {
                    Text(
                        text = "Open: %.2f".format(open),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            quote.high?.let { high ->
                item {
                    Text(
                        text = "High: %.2f".format(high),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            quote.low?.let { low ->
                item {
                    Text(
                        text = "Low: %.2f".format(low),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item {
                Text(
                    text = "As of ${formatFetchedAt(quote.fetchedAt)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        item {
            Button(
                onClick = {
                    viewModel.removeFromWatchlist()
                    onBack()
                }
            ) {
                Text("Remove from list")
            }
        }
        item {
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * Simplified ambient detail: symbol + price in monochrome.
 */
@Composable
private fun AmbientDetailContent(quote: StockQuote?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (quote != null) {
            Text(
                text = quote.symbol,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = "%.2f".format(quote.price),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "As of ${formatFetchedAt(quote.fetchedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}
