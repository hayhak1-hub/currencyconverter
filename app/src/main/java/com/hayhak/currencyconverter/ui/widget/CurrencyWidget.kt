package com.hayhak.currencyconverter.ui.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrencyWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun repository(): ExchangeRateRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )
        val repository = entryPoint.repository()

        provideContent {
            val exchangeRate by repository.getLatestRates("USD").collectAsState(initial = null)

            val rates   = exchangeRate?.rates ?: emptyMap()
            val tryRate = rates["TRY"] ?: 0.0
            fun crossRate(code: String) =
                rates[code]?.takeIf { it > 0 }?.let { tryRate / it } ?: 0.0

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(BG)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                // ── Başlık + Yenile butonu ──────────────────────────
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        context.getString(com.hayhak.currencyconverter.R.string.widget_live_rates),
                        style    = TextStyle(
                            color      = LABEL,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        "↻",
                        style    = TextStyle(color = RATE, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.clickable(actionRunCallback<WidgetRefreshAction>())
                    )
                }

                Spacer(GlanceModifier.height(10.dp))

                // ── Kur satırları ───────────────────────────────────
                RateRow("$",  "USD", tryRate)
                Spacer(GlanceModifier.height(5.dp))
                RateRow("€",  "EUR", crossRate("EUR"))
                Spacer(GlanceModifier.height(5.dp))
                RateRow("£",  "GBP", crossRate("GBP"))
                Spacer(GlanceModifier.height(5.dp))
                RateRow("Fr", "CHF", crossRate("CHF"))

                Spacer(GlanceModifier.defaultWeight())

                // ── Son güncelleme ──────────────────────────────────
                val ts = exchangeRate?.timestamp
                Text(
                    text  = if (ts != null) formatTime(context, ts) else context.getString(com.hayhak.currencyconverter.R.string.widget_no_data),
                    style = TextStyle(color = SUBTLE, fontSize = 9.sp)
                )
            }
        }
    }

    companion object {
        val BG     = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF0D1829))
        val LABEL  = ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFFE6E1E5))
        val RATE   = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF4FC3F7))
        val SUBTLE = ColorProvider(day = Color(0xFF79747E), night = Color(0xFF938F99))

        fun formatTime(context: Context, ts: Long): String {
            val mins = ((System.currentTimeMillis() - ts) / 60_000).toInt()
            return when {
                mins < 1  -> context.getString(com.hayhak.currencyconverter.R.string.dashboard_just_now)
                mins < 60 -> context.resources.getQuantityString(
                    com.hayhak.currencyconverter.R.plurals.minutes_ago, mins, mins
                )
                else      -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RateRow(symbol: String, code: String, rate: Double) {
    Row(
        modifier          = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = "$symbol $code",
            style    = TextStyle(
                color      = ColorProvider(day = Color(0xFF49454F), night = Color(0xFFCAC4D0)),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text  = if (rate > 0) "%.4f ₺".format(rate) else "—",
            style = TextStyle(
                color      = CurrencyWidget.RATE,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// ── Yenileme aksiyonu ──────────────────────────────────────────────────
class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, CurrencyWidget.WidgetEntryPoint::class.java
        )
        try {
            entryPoint.repository().refreshRates("USD")
        } catch (_: Exception) { }
        CurrencyWidget().update(context, glanceId)
    }
}
