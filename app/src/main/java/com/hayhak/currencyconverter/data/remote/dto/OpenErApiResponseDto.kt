package com.hayhak.currencyconverter.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Response from ExchangeRate-API open access: https://open.er-api.com/v6/latest/{base} */
data class OpenErApiResponseDto(
    @SerializedName("result") val result: String,
    @SerializedName("base_code") val baseCode: String,
    @SerializedName("time_last_update_unix") val timeLastUpdateUnix: Long,
    @SerializedName("rates") val rates: Map<String, Double>
)
