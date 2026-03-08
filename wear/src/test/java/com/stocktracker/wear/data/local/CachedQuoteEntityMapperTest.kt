package com.stocktracker.wear.data.local

import com.stocktracker.wear.data.local.toDomain
import com.stocktracker.wear.data.local.toEntity
import com.stocktracker.wear.data.local.entity.CachedQuoteEntity
import com.stocktracker.wear.domain.StockQuote
import org.junit.Assert.*
import org.junit.Test

class CachedQuoteEntityMapperTest {

    private val sampleEntity = CachedQuoteEntity(
        symbol = "AAPL",
        price = 150.00,
        change = 2.50,
        changePercent = 1.69,
        open = 148.00,
        high = 151.00,
        low = 147.50,
        previousClose = 147.50,
        latestTradingDay = "2025-01-15",
        fetchedAt = 1700000000000L
    )

    private val sampleQuote = StockQuote(
        symbol = "AAPL",
        price = 150.00,
        change = 2.50,
        changePercent = 1.69,
        open = 148.00,
        high = 151.00,
        low = 147.50,
        previousClose = 147.50,
        latestTradingDay = "2025-01-15",
        fetchedAt = 1700000000000L
    )

    @Test
    fun `toDomain maps all entity fields to StockQuote`() {
        val result = sampleEntity.toDomain()

        assertEquals(sampleQuote.symbol, result.symbol)
        assertEquals(sampleQuote.price, result.price, 0.001)
        assertEquals(sampleQuote.change, result.change, 0.001)
        assertEquals(sampleQuote.changePercent, result.changePercent, 0.001)
        assertEquals(sampleQuote.open, result.open)
        assertEquals(sampleQuote.high, result.high)
        assertEquals(sampleQuote.low, result.low)
        assertEquals(sampleQuote.previousClose, result.previousClose)
        assertEquals(sampleQuote.latestTradingDay, result.latestTradingDay)
        assertEquals(sampleQuote.fetchedAt, result.fetchedAt)
    }

    @Test
    fun `toEntity maps all StockQuote fields to entity`() {
        val result = sampleQuote.toEntity()

        assertEquals(sampleEntity.symbol, result.symbol)
        assertEquals(sampleEntity.price, result.price, 0.001)
        assertEquals(sampleEntity.change, result.change, 0.001)
        assertEquals(sampleEntity.changePercent, result.changePercent, 0.001)
        assertEquals(sampleEntity.open, result.open)
        assertEquals(sampleEntity.high, result.high)
        assertEquals(sampleEntity.low, result.low)
        assertEquals(sampleEntity.previousClose, result.previousClose)
        assertEquals(sampleEntity.latestTradingDay, result.latestTradingDay)
        assertEquals(sampleEntity.fetchedAt, result.fetchedAt)
    }

    @Test
    fun `round trip entity to domain and back preserves all fields`() {
        val roundTripped = sampleEntity.toDomain().toEntity()

        assertEquals(sampleEntity, roundTripped)
    }

    @Test
    fun `round trip domain to entity and back preserves all fields`() {
        val roundTripped = sampleQuote.toEntity().toDomain()

        assertEquals(sampleQuote, roundTripped)
    }

    @Test
    fun `toDomain handles null optional fields`() {
        val entityWithNulls = sampleEntity.copy(
            open = null,
            high = null,
            low = null,
            previousClose = null,
            latestTradingDay = null
        )

        val result = entityWithNulls.toDomain()

        assertNull(result.open)
        assertNull(result.high)
        assertNull(result.low)
        assertNull(result.previousClose)
        assertNull(result.latestTradingDay)
    }
}
