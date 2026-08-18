package com.hayhak.currencyconverter.data.remote.api

import com.hayhak.currencyconverter.data.remote.dto.FrankfurterRateDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {

    /** Historical rates from Frankfurter (ECB reference data). */
    @GET("rates")
    suspend fun getLatestRates(
        @Query("base") base: String = "USD"
    ): List<FrankfurterRateDto>

    @GET("rates")
    suspend fun getHistoricalRates(
        @Query("base") base: String,
        @Query("quotes") quotes: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("group") group: String? = null
    ): List<FrankfurterRateDto>
}
