package com.hayhak.currencyconverter.data.remote.dto

import com.google.gson.annotations.SerializedName

/** USD spot price per troy ounce from https://api.gold-api.com/price/{symbol} */
data class GoldPriceDto(
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("name") val name: String? = null
)
