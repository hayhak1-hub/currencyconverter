package com.hayhak.currencyconverter.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.ui.theme.TrendDown
import com.hayhak.currencyconverter.ui.theme.TrendDownDark
import com.hayhak.currencyconverter.ui.theme.TrendUp
import com.hayhak.currencyconverter.ui.theme.TrendUpDark

/**
 * Day-over-day extremes. In every row, [CurrencyRow.trend] is the change in the currency's *value*:
 * positive means it bought more than yesterday. (The Rates list shows the opposite, the change of
 * the quoted rate itself, so a rising rate there is a falling value here.)
 */
internal data class Movers(val gainers: List<CurrencyRow>, val losers: List<CurrencyRow>) {
    val isEmpty get() = gainers.isEmpty() && losers.isEmpty()
}

/** Changes beyond this are treated as bad data. */
internal const val MAX_PLAUSIBLE_PERCENT = 10.0

/** Widely traded currencies. Exotic or pegged ones swing on thin, stale quotes and would dominate the list. */
internal val MAJOR_CODES = setOf(
    "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD", "TRY", "CNY", "HKD", "SGD", "SEK", "NOK", "DKK",
    "PLN", "CZK", "HUF", "RON", "MXN", "BRL", "ZAR", "INR", "KRW", "THB", "IDR", "MYR", "PHP", "ILS", "AED",
    "SAR", "RUB", "UAH", "EGP"
)

/**
 * A quote of "units per base" that rises by [rateChangePercent] means the currency buys less of the
 * base, so its value falls. Converts the rate change into the currency's value change.
 */
internal fun valueChangePercent(rateChangePercent: Double): Double =
    if (rateChangePercent <= -100.0) Double.NaN else (100.0 / (100.0 + rateChangePercent) - 1.0) * 100.0

private fun List<CurrencyRow>.plausible(allowed: Set<String>?) =
    filter { allowed == null || it.code in allowed }
        .filter { it.trend.isFinite() && kotlin.math.abs(it.trend) <= MAX_PLAUSIBLE_PERCENT }

private fun List<CurrencyRow>.usable(allowed: Set<String>?) = plausible(allowed).filter { it.trend != 0.0 }

private fun split(rows: List<CurrencyRow>, count: Int) = Movers(
    gainers = rows.filter { it.trend > 0 }.sortedByDescending { it.trend }.take(count),
    losers = rows.filter { it.trend < 0 }.sortedBy { it.trend }.take(count)
)

/** Biggest value gainers and losers against the base currency. [rows] carry the rate change. */
internal fun topMovers(rows: List<CurrencyRow>, count: Int = 3, allowed: Set<String>? = MAJOR_CODES): Movers =
    split(rows.usable(allowed).map { it.copy(trend = valueChangePercent(it.trend)) }, count)

/**
 * Base-independent strength: each currency's average value change against all the others in the
 * set. The base currency takes part with an unchanged rate (it is the unit the others are quoted in).
 */
internal fun strengthMovers(
    rows: List<CurrencyRow>,
    baseCode: String,
    count: Int = 3,
    allowed: Set<String>? = MAJOR_CODES
): Movers {
    val factors = LinkedHashMap<String, Double>()
    rows.plausible(allowed).forEach { factors[it.code] = 1.0 + valueChangePercent(it.trend) / 100.0 }
    if (allowed == null || baseCode in allowed) factors[baseCode] = 1.0
    if (factors.size < 3) return Movers(emptyList(), emptyList())

    val byCode = rows.associateBy { it.code }
    val scored = factors.mapNotNull { (code, factor) ->
        val others = factors.filterKeys { it != code }.values
        val strength = (others.sumOf { factor / it } / others.size - 1.0) * 100.0
        val template = byCode[code] ?: return@mapNotNull null
        template.copy(trend = strength)
    }.filter { it.trend.isFinite() && it.trend != 0.0 }
    return split(scored, count)
}

@Composable
internal fun MoversCard(
    vsBase: Movers,
    strength: Movers,
    baseCode: String,
    modifier: Modifier = Modifier
) {
    if (vsBase.isEmpty && strength.isEmpty) return
    var showStrength by rememberSaveable { mutableStateOf(false) }
    val current = if (showStrength && !strength.isEmpty) strength else vsBase
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showStrength,
                    onClick = { showStrength = false },
                    label = { Text(stringResource(R.string.movers_vs_base, baseCode)) }
                )
                if (!strength.isEmpty) {
                    FilterChip(
                        selected = showStrength,
                        onClick = { showStrength = true },
                        label = { Text(stringResource(R.string.movers_strength)) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MoverColumn(
                    stringResource(if (showStrength) R.string.movers_strongest else R.string.movers_gainers),
                    current.gainers,
                    Modifier.weight(1f)
                )
                MoverColumn(
                    stringResource(if (showStrength) R.string.movers_weakest else R.string.movers_losers),
                    current.losers,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MoverColumn(title: String, rows: List<CurrencyRow>, modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        rows.forEach { row ->
            val color = if (row.trend > 0) {
                if (isDark) TrendUpDark else TrendUp
            } else {
                if (isDark) TrendDownDark else TrendDown
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${row.flag} ${row.code}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("%+.2f%%".format(row.trend), style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
    }
}
