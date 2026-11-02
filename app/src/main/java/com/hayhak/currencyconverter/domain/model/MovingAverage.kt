package com.hayhak.currencyconverter.domain.model

import java.util.concurrent.TimeUnit

/**
 * Simple moving average over a trailing window of [windowDays] calendar days.
 *
 * The series can be sparse (weekly or monthly points on long ranges), so the window is measured in
 * time, not in points. A point is only produced once the data covers the full window, which keeps
 * the line from starting with misleading short-window averages. [data] must be sorted by date.
 */
fun movingAverage(data: List<HistoricalRate>, windowDays: Int): List<HistoricalRate> {
    if (windowDays <= 0 || data.size < 2) return emptyList()
    val windowMs = TimeUnit.DAYS.toMillis(windowDays.toLong())
    val firstDate = data.first().date
    val result = ArrayList<HistoricalRate>(data.size)
    var start = 0
    var sum = 0.0
    for (end in data.indices) {
        sum += data[end].rate
        // Drop points that fell out of the trailing window.
        while (data[end].date - data[start].date > windowMs) {
            sum -= data[start].rate
            start++
        }
        if (data[end].date - firstDate >= windowMs) {
            result += HistoricalRate(data[end].date, sum / (end - start + 1))
        }
    }
    return result
}
