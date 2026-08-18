package com.hayhak.currencyconverter.data.remote.api

import com.hayhak.currencyconverter.data.remote.dto.OpenErApiResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Free open-access live rates from ExchangeRate-API (no API key). */
interface LiveExchangeRateApi {

    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): OpenErApiResponseDto
}
