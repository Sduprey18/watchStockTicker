package com.stocktracker.wear.data.remote

import com.stocktracker.wear.data.remote.dto.GlobalQuoteDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApi {

    @GET("query")
    suspend fun getGlobalQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apikey: String
    ): GlobalQuoteDto
}
