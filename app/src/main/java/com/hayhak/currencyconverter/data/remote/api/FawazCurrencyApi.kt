package com.hayhak.currencyconverter.data.remote.api

import com.hayhak.currencyconverter.data.remote.dto.FawazUsdDto
import retrofit2.http.GET

/** Fallback FX/metal table (lowercase codes). XAU here is troy ounces per 1 USD. */
interface FawazCurrencyApi {
    @GET("npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.min.json")
    suspend fun getUsd(): FawazUsdDto
}
