package com.hayhak.currencyconverter.data.remote.api

import com.hayhak.currencyconverter.data.remote.dto.GoldPriceDto
import retrofit2.http.GET
import retrofit2.http.Path

interface GoldPriceApi {
    @GET("price/{symbol}")
    suspend fun getPrice(@Path("symbol") symbol: String): GoldPriceDto
}
