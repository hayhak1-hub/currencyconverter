package com.hayhak.currencyconverter.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hayhak.currencyconverter.MainActivity
import com.hayhak.currencyconverter.domain.model.getFlagEmoji
import com.hayhak.currencyconverter.domain.model.quotePerUnit
import java.util.concurrent.TimeUnit
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class CurrencyWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun repository(): ExchangeRateRepository
        fun userPrefs(): UserPreferencesRepository
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL,
            MEDIUM,
            LARGE
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java
        )
        val repository = entryPoint.repository()
        val userPrefs = entryPoint.userPrefs()

        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val yesterday = runCatching {
            repository.getRatesBefore("USD", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(20))?.rates
        }.getOrNull() ?: emptyMap()
        val initial = combine(
            repository.getLatestRates("USD"),
            userPrefs.widgetBase,
            userPrefs.widgetCodes
        ) { rates, base, codes -> Triple(rates, base, codes) }.first()

        // Trend lines are drawn on the large layout only; they need a few days of stored history.
        val sparkQuote = WidgetInstanceStore.load(context, appWidgetId)?.first ?: initial.second
        val sparkCodes = (WidgetInstanceStore.load(context, appWidgetId)?.second ?: initial.third)
            .filter { it != sparkQuote }.take(4)
        val sparks = loadSparklines(repository, sparkCodes, sparkQuote)

        provideContent {
            val snapshot by combine(
                repository.getLatestRates("USD"),
                userPrefs.widgetBase,
                userPrefs.widgetCodes
            ) { rates, base, codes -> Triple(rates, base, codes) }
                .collectAsState(initial = initial)

            val (exchangeRate, globalQuote, globalCodes) = snapshot
            val instance = WidgetInstanceStore.load(context, appWidgetId)
            val quote = instance?.first ?: globalQuote
            val codes = instance?.second ?: globalCodes
            val usdRates = exchangeRate?.rates ?: emptyMap()
            val size = LocalSize.current
            val maxRows = when {
                size.height < 80.dp -> 1
                size.height < 140.dp -> 2
                else -> 4
            }
            val rows = codes.filter { it != quote }.take(maxRows)
            val openApp = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAV, MainActivity.NAV_CONVERTER)
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(BG)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clickable(actionStartActivity(openApp)),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${context.getString(com.hayhak.currencyconverter.R.string.widget_live_rates)} · $quote",
                        style = TextStyle(color = LABEL, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        "✎",
                        style = TextStyle(color = RATE, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.clickable(actionRunCallback<WidgetEditAction>())
                    )
                    Text(
                        "↻",
                        style = TextStyle(color = RATE, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.clickable(actionRunCallback<WidgetRefreshAction>())
                    )
                }

                Spacer(GlanceModifier.height(8.dp))

                rows.forEachIndexed { i, code ->
                    val rate = quotePerUnit(code, quote, usdRates)
                    val previous = quotePerUnit(code, quote, yesterday)
                    val change = if (rate != null && previous != null && previous != 0.0) {
                        (rate - previous) / previous * 100.0
                    } else null
                    val openPair = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_NAV, MainActivity.NAV_CONVERTER)
                        putExtra(MainActivity.EXTRA_FROM, code)
                        putExtra(MainActivity.EXTRA_TARGET, quote)
                    }
                    RateRow(getFlagEmoji(code), code, rate, quote, change, openPair, if (maxRows >= 4) sparks[code] else null)
                    if (i != rows.lastIndex) Spacer(GlanceModifier.height(5.dp))
                }

                Spacer(GlanceModifier.defaultWeight())
                val ts = exchangeRate?.timestamp
                Text(
                    text = if (ts != null) formatTime(context, ts) else context.getString(com.hayhak.currencyconverter.R.string.widget_no_data),
                    style = TextStyle(color = SUBTLE, fontSize = 9.sp)
                )
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 48.dp)
        val MEDIUM = DpSize(180.dp, 110.dp)
        val LARGE = DpSize(250.dp, 180.dp)

        val BG = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF0D1829))
        val LABEL = ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFFE6E1E5))
        val RATE = ColorProvider(day = Color(0xFF1565C0), night = Color(0xFF4FC3F7))
        val SUBTLE = ColorProvider(day = Color(0xFF79747E), night = Color(0xFF938F99))
        val UP = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784))
        val DOWN = ColorProvider(day = Color(0xFFC62828), night = Color(0xFFEF9A9A))

        fun formatTime(context: Context, ts: Long): String {
            val mins = ((System.currentTimeMillis() - ts) / 60_000).toInt()
            return when {
                mins < 1 -> context.getString(com.hayhak.currencyconverter.R.string.dashboard_just_now)
                mins < 60 -> context.resources.getQuantityString(
                    com.hayhak.currencyconverter.R.plurals.minutes_ago, mins, mins
                )
                else -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(ts))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RateRow(
    flag: String,
    code: String,
    rate: Double?,
    quote: String,
    change: Double?,
    openPair: Intent,
    spark: android.graphics.Bitmap? = null
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openPair)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$flag $code",
            style = TextStyle(
                color = ColorProvider(day = Color(0xFF49454F), night = Color(0xFFCAC4D0)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        if (spark != null) {
            Image(
                provider = ImageProvider(spark),
                contentDescription = null,
                modifier = GlanceModifier.width(56.dp).height(22.dp).padding(end = 6.dp)
            )
        }
        Column(horizontalAlignment = Alignment.Horizontal.End) {
            Text(
                text = if (rate != null && rate > 0) "%.4f %s".format(rate, quote) else "—",
                style = TextStyle(color = CurrencyWidget.RATE, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            if (change != null) {
                Text(
                    text = "%+.1f%%".format(change),
                    style = TextStyle(
                        color = if (change >= 0) CurrencyWidget.UP else CurrencyWidget.DOWN,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
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

class WidgetEditAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        val appWidgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val intent = Intent(context, WidgetConfigureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(WidgetConfigureActivity.EXTRA_EDITING, true)
        }
        context.startActivity(intent)
    }
}

private val SPARK_COLOR = android.graphics.Color.parseColor("#4FC3F7")

/**
 * Seven-day trend per row, rendered as bitmaps. A missing history is fetched once with a short
 * limit so the widget never waits long on the network; rows without data simply get no line.
 */
private suspend fun loadSparklines(
    repository: ExchangeRateRepository,
    codes: List<String>,
    quote: String
): Map<String, android.graphics.Bitmap> {
    if (codes.isEmpty()) return emptyMap()
    suspend fun usdSeries(code: String) =
        if (code == "USD") null else runCatching {
            repository.getHistoricalRates("USD", code, 7).first()
        }.getOrDefault(emptyList())

    var quoteSeries = usdSeries(quote)
    var codeSeries = codes.associateWith { usdSeries(it) }
    val missing = (codes + quote).filter { it != "USD" && ((if (it == quote) quoteSeries else codeSeries[it])?.size ?: 0) < 3 }
    if (missing.isNotEmpty()) {
        kotlinx.coroutines.withTimeoutOrNull(8_000) {
            runCatching { repository.refreshHistoricalRates("USD", missing.distinct().joinToString(","), 7) }
        }
        quoteSeries = usdSeries(quote)
        codeSeries = codes.associateWith { usdSeries(it) }
    }

    val density = 3 // fixed render scale; the image is scaled to its dp size
    val out = LinkedHashMap<String, android.graphics.Bitmap>()
    codes.forEach { code ->
        val series = com.hayhak.currencyconverter.domain.model.crossSeries(codeSeries[code], quoteSeries)
        renderSparkline(series.map { it.rate }, 56 * density, 22 * density, SPARK_COLOR)?.let { out[code] = it }
    }
    return out
}
