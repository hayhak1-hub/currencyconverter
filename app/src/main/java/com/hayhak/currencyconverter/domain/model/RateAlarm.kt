package com.hayhak.currencyconverter.domain.model

data class RateAlarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val baseCode: String,
    val targetCode: String,
    val threshold: Double,
    val isAbove: Boolean,
    val isEnabled: Boolean = true,
    val repeating: Boolean = false,
    val lastFiredAt: Long = 0L,
    /** "threshold" or "percent". Null on alarms saved before percent alerts existed. */
    val kind: String? = null,
    val percent: Double = 0.0
) {
    val isPercent: Boolean get() = kind == KIND_PERCENT

    companion object {
        const val KIND_THRESHOLD = "threshold"
        const val KIND_PERCENT = "percent"
    }
}
