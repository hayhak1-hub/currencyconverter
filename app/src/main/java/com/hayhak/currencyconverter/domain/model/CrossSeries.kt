package com.hayhak.currencyconverter.domain.model

import java.util.concurrent.TimeUnit

/**
 * Value of one unit of a currency in another over time, built from two USD-based histories
 * ("units per USD"). A null series stands for USD itself, whose rate is always 1.
 *
 * Only days present in every given series are used, so a gap in one of them does not skew the line.
 * The result is sorted by date.
 */
fun crossSeries(code: List<HistoricalRate>?, quote: List<HistoricalRate>?): List<HistoricalRate> {
    if (code == null && quote == null) return emptyList()
    fun byDay(series: List<HistoricalRate>) =
        series.filter { it.rate > 0 && it.rate.isFinite() }.associateBy { TimeUnit.MILLISECONDS.toDays(it.date) }

    val codeByDay = code?.let(::byDay)
    val quoteByDay = quote?.let(::byDay)
    val days = when {
        codeByDay != null && quoteByDay != null -> codeByDay.keys.intersect(quoteByDay.keys)
        codeByDay != null -> codeByDay.keys
        else -> quoteByDay!!.keys
    }
    return days.sorted().map { day ->
        val codeRate = codeByDay?.get(day)?.rate ?: 1.0
        val quoteRate = quoteByDay?.get(day)?.rate ?: 1.0
        val date = (codeByDay?.get(day) ?: quoteByDay!![day]!!).date
        HistoricalRate(date, quoteRate / codeRate)
    }
}
