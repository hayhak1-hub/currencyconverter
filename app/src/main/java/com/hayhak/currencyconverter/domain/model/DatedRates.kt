package com.hayhak.currencyconverter.domain.model

data class DatedRates(
    val date: String,
    val rates: Map<String, Double>
)
