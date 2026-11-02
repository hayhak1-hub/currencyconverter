package com.hayhak.currencyconverter.domain.model

data class QuietHours(
    val enabled: Boolean = false,
    val startMinute: Int = 23 * 60,
    val endMinute: Int = 8 * 60
)
