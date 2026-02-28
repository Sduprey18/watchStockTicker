package com.stocktracker.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.stocktracker.wear.domain.StockQuote

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
}
