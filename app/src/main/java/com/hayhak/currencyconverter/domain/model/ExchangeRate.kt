package com.hayhak.currencyconverter.domain.model

data class ExchangeRate(
    val baseCurrency: String,
    val rates: Map<String, Double>,
    val timestamp: Long
)

data class HistoricalRate(
    val date: Long,
    val rate: Double
)
