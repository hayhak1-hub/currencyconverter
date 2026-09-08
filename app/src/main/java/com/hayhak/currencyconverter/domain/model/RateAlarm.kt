package com.hayhak.currencyconverter.domain.model

data class RateAlarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val baseCode: String,
    val targetCode: String,
    val threshold: Double,
    val isAbove: Boolean,
    val isEnabled: Boolean = true,
    val repeating: Boolean = false,
    val lastFiredAt: Long = 0L
)
