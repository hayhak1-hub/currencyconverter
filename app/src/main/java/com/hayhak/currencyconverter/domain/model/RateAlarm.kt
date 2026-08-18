package com.hayhak.currencyconverter.domain.model

data class RateAlarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val baseCode: String,
    val targetCode: String,
    val threshold: Double,
    val isAbove: Boolean, // true if trigger when rate > threshold, false if below
    val isEnabled: Boolean = true
)
