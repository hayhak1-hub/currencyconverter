package com.hayhak.currencyconverter.data.local.entity

import androidx.room.Entity

@Entity(tableName = "latest_rates", primaryKeys = ["baseCurrency", "targetCurrency"])
data class LatestRateEntity(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val timestamp: Long
)
