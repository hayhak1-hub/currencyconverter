package com.hayhak.currencyconverter.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** Day-over-day extremes. Changes beyond [MAX_PLAUSIBLE_PERCENT] are treated as bad data. */
internal data class Movers(val gainers: List<CurrencyRow>, val losers: List<CurrencyRow>) {
    val isEmpty get() = gainers.isEmpty() && losers.isEmpty()
}

internal const val MAX_PLAUSIBLE_PERCENT = 10.0

/** Widely traded currencies. Exotic or pegged ones swing on thin, stale quotes and would dominate the list. */
internal val MAJOR_CODES = setOf(
    "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD", "TRY", "CNY", "HKD", "SGD", "SEK", "NOK", "DKK",
    "PLN", "CZK", "HUF", "RON", "MXN", "BRL", "ZAR", "INR", "KRW", "THB", "IDR", "MYR", "PHP", "ILS", "AED",
    "SAR", "RUB", "UAH", "EGP"
)

internal fun topMovers(rows: List<CurrencyRow>, count: Int = 3, allowed: Set<String>? = MAJOR_CODES): Movers {
    val valid = rows.filter { allowed == null || it.code in allowed }.filter { it.trend.isFinite() && it.trend != 0.0 && kotlin.math.abs(it.trend) <= MAX_PLAUSIBLE_PERCENT }
    return Movers(
        gainers = valid.filter { it.trend > 0 }.sortedByDescending { it.trend }.take(count),
        losers = valid.filter { it.trend < 0 }.sortedBy { it.trend }.take(count)
    )
}

@Composable
internal fun MoversCard(movers: Movers, modifier: Modifier = Modifier) {
    if (movers.isEmpty) return
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MoverColumn(stringResource(R.string.movers_gainers), movers.gainers, Modifier.weight(1f))
            MoverColumn(stringResource(R.string.movers_losers), movers.losers, Modifier.weight(1f))
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
