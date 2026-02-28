package com.stocktracker.wear.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GlobalQuoteDto(
    @SerializedName("Global Quote") val globalQuote: GlobalQuoteInnerDto?
)

data class GlobalQuoteInnerDto(
    @SerializedName("01. symbol") val symbol: String? = null,
    @SerializedName("02. open") val open: String? = null,
    @SerializedName("03. high") val high: String? = null,
    @SerializedName("04. low") val low: String? = null,
    @SerializedName("05. price") val price: String? = null,
    @SerializedName("06. volume") val volume: String? = null,
    @SerializedName("07. latest trading day") val latestTradingDay: String? = null,
    @SerializedName("08. previous close") val previousClose: String? = null,
    @SerializedName("09. change") val change: String? = null,
    @SerializedName("10. change percent") val changePercent: String? = null
)
