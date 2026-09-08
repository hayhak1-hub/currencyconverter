package com.hayhak.currencyconverter.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FawazUsdDto(
    @SerializedName("usd") val usd: Map<String, Double> = emptyMap()
)
