package com.stocktracker.wear.data.remote

import com.stocktracker.wear.data.remote.dto.GlobalQuoteDto
import com.stocktracker.wear.domain.StockQuote

fun GlobalQuoteDto.toDomain(symbol: String): StockQuote? {
    val q = globalQuote ?: return null
    val price = q.price?.toDoubleOrNull() ?: return null
    val change = q.change?.toDoubleOrNull() ?: 0.0
    val changePercentStr = q.changePercent?.replace("%", "")?.trim()
    val changePercent = changePercentStr?.toDoubleOrNull() ?: 0.0
    return StockQuote(
        symbol = q.symbol ?: symbol,
        price = price,
        change = change,
        changePercent = changePercent,
        open = q.open?.toDoubleOrNull(),
        high = q.high?.toDoubleOrNull(),
        low = q.low?.toDoubleOrNull(),
        previousClose = q.previousClose?.toDoubleOrNull(),
        latestTradingDay = q.latestTradingDay,
        fetchedAt = System.currentTimeMillis()
    )
}
