package com.stocktracker.wear.data.local

import com.stocktracker.wear.data.local.entity.CachedQuoteEntity
import com.stocktracker.wear.domain.StockQuote

fun CachedQuoteEntity.toDomain(): StockQuote = StockQuote(
    symbol = symbol,
    price = price,
    change = change,
    changePercent = changePercent,
    open = open,
    high = high,
    low = low,
    previousClose = previousClose,
    latestTradingDay = latestTradingDay,
    fetchedAt = fetchedAt
)

fun StockQuote.toEntity(): CachedQuoteEntity = CachedQuoteEntity(
    symbol = symbol,
    price = price,
    change = change,
    changePercent = changePercent,
    open = open,
    high = high,
    low = low,
    previousClose = previousClose,
    latestTradingDay = latestTradingDay,
    fetchedAt = fetchedAt
)
