package com.hayhak.currencyconverter.domain.model

/** Where the latest rate sits inside the loaded period, to help judge whether now is a good moment to exchange. */
data class RateTiming(
    /** Share of earlier points in the period whose rate was lower than the latest one, 0..100. */
    val percentile: Int,
    /** Latest rate versus the period average, in percent. */
    val vsAveragePercent: Double,
    /** 0 at the period low, 1 at the period high. */
    val positionInRange: Float
)

private const val MIN_POINTS = 10

/** Returns null when there is too little data (or a flat series) for the figure to mean anything. */
fun rateTiming(data: List<HistoricalRate>): RateTiming? {
    if (data.size < MIN_POINTS) return null
    val rates = data.map { it.rate }
    if (rates.any { !it.isFinite() || it <= 0.0 }) return null
    val latest = rates.last()
    val earlier = rates.dropLast(1)
    val average = rates.average()
    val min = rates.min()
    val max = rates.max()
    if (max - min <= 0.0) return null
    val lower = earlier.count { it < latest }
    return RateTiming(
        percentile = (lower * 100.0 / earlier.size).toInt().coerceIn(0, 100),
        vsAveragePercent = (latest - average) / average * 100.0,
        positionInRange = ((latest - min) / (max - min)).toFloat().coerceIn(0f, 1f)
    )
}
