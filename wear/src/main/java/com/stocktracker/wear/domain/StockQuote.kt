package com.stocktracker.wear.domain

data class StockQuote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val previousClose: Double? = null,
    val latestTradingDay: String? = null,
    val fetchedAt: Long = System.currentTimeMillis()
)
