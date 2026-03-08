package com.stocktracker.wear.data.remote

import com.stocktracker.wear.data.remote.dto.GlobalQuoteDto
import com.stocktracker.wear.data.remote.dto.GlobalQuoteInnerDto
import org.junit.Assert.*
import org.junit.Test

class QuoteMapperTest {

    @Test
    fun `toDomain maps valid DTO correctly`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                symbol = "AAPL",
                open = "150.00",
                high = "155.00",
                low = "149.00",
                price = "152.50",
                volume = "1000000",
                latestTradingDay = "2025-01-15",
                previousClose = "151.00",
                change = "1.50",
                changePercent = "0.9934%"
            )
        )

        val result = dto.toDomain("AAPL")

        assertNotNull(result)
        result!!
        assertEquals("AAPL", result.symbol)
        assertEquals(152.50, result.price, 0.001)
        assertEquals(1.50, result.change, 0.001)
        assertEquals(0.9934, result.changePercent, 0.0001)
        assertEquals(150.00, result.open!!, 0.001)
        assertEquals(155.00, result.high!!, 0.001)
        assertEquals(149.00, result.low!!, 0.001)
        assertEquals(151.00, result.previousClose!!, 0.001)
        assertEquals("2025-01-15", result.latestTradingDay)
    }

    @Test
    fun `toDomain returns null when globalQuote is null`() {
        val dto = GlobalQuoteDto(globalQuote = null)

        val result = dto.toDomain("AAPL")

        assertNull(result)
    }

    @Test
    fun `toDomain returns null when price is null`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                price = null,
                change = "1.50",
                changePercent = "1.00%"
            )
        )

        val result = dto.toDomain("AAPL")

        assertNull(result)
    }

    @Test
    fun `toDomain returns null when price is not a valid number`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                price = "not_a_number",
                change = "1.50",
                changePercent = "1.00%"
            )
        )

        val result = dto.toDomain("AAPL")

        assertNull(result)
    }

    @Test
    fun `toDomain defaults change to zero when change is null`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                price = "100.00",
                change = null,
                changePercent = null
            )
        )

        val result = dto.toDomain("AAPL")

        assertNotNull(result)
        result!!
        assertEquals(0.0, result.change, 0.001)
        assertEquals(0.0, result.changePercent, 0.001)
    }

    @Test
    fun `toDomain strips percent sign from changePercent`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                price = "100.00",
                change = "2.00",
                changePercent = "2.50%"
            )
        )

        val result = dto.toDomain("AAPL")

        assertNotNull(result)
        assertEquals(2.50, result!!.changePercent, 0.001)
    }

    @Test
    fun `toDomain uses fallback symbol when DTO symbol is null`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                symbol = null,
                price = "100.00"
            )
        )

        val result = dto.toDomain("MSFT")

        assertNotNull(result)
        assertEquals("MSFT", result!!.symbol)
    }

    @Test
    fun `toDomain uses DTO symbol when present`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                symbol = "GOOG",
                price = "100.00"
            )
        )

        val result = dto.toDomain("MSFT")

        assertNotNull(result)
        assertEquals("GOOG", result!!.symbol)
    }

    @Test
    fun `toDomain handles null optional fields`() {
        val dto = GlobalQuoteDto(
            globalQuote = GlobalQuoteInnerDto(
                price = "100.00",
                open = null,
                high = null,
                low = null,
                previousClose = null,
                latestTradingDay = null
            )
        )

        val result = dto.toDomain("AAPL")

        assertNotNull(result)
        result!!
        assertNull(result.open)
        assertNull(result.high)
        assertNull(result.low)
        assertNull(result.previousClose)
        assertNull(result.latestTradingDay)
    }
}
