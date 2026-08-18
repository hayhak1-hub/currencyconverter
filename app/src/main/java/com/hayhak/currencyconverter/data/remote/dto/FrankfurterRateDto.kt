package com.hayhak.currencyconverter.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Single row from Frankfurter v2 `/rates` (flat array). */
data class FrankfurterRateDto(
    @SerializedName("date") val date: String,
    @SerializedName("base") val base: String,
    @SerializedName("quote") val quote: String,
    @SerializedName("rate") val rate: Double
)
