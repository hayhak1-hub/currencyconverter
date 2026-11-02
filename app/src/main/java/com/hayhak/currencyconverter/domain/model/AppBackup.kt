package com.hayhak.currencyconverter.domain.model

data class AppBackup(
    val version: Int = 1,
    val favorites: List<String> = emptyList(),
    val alarms: List<RateAlarm> = emptyList(),
    val widgetBase: String = "USD",
    val widgetCodes: List<String> = emptyList(),
    val spreadPercent: Double = 1.0,
    val quietHoursEnabled: Boolean = false,
    val quietStartMinute: Int = 23 * 60,
    val quietEndMinute: Int = 8 * 60,
    val savedPairs: List<String>? = null,
    val morningDigest: Boolean? = null
)
