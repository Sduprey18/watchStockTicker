package com.stocktracker.wear.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_quotes")
data class CachedQuoteEntity(
    @PrimaryKey val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val previousClose: Double?,
    val latestTradingDay: String?,
    val fetchedAt: Long
)
