package com.hayhak.currencyconverter.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.HistoricalRate
import com.hayhak.currencyconverter.domain.model.getFlagEmoji
import com.hayhak.currencyconverter.domain.model.movingAverage
import com.hayhak.currencyconverter.domain.model.rateTiming
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    isWide: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSma50 by rememberSaveable { mutableStateOf(false) }
    var showSma200 by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val csvSaved = stringResource(R.string.history_csv_saved)
    val csvFailed = stringResource(R.string.history_csv_failed)

    // Fixed popular pairs, then the user's favorites against USD, and always the current pair.
    val pairs = remember(state.favorites, state.baseCurrency, state.targetCurrency) {
        val list = linkedSetOf(
            "USD" to "EUR",
            "EUR" to "USD",
            "USD" to "GBP",
            "USD" to "JPY",
            "EUR" to "GBP",
            "GBP" to "JPY"
        )
        state.favorites.sorted().filter { it != "USD" }.forEach { list.add("USD" to it) }
        list.add(state.baseCurrency to state.targetCurrency)
        list.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 16.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.baseInfo?.let { base ->
                    CurrencySelector(
                        label = stringResource(R.string.converter_from),
                        currency = base,
                        onSelect = viewModel::selectBase,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = viewModel::swapCurrencies) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.cd_swap))
                }
                state.targetInfo?.let { target ->
                    CurrencySelector(
                        label = stringResource(R.string.converter_to),
                        currency = target,
                        onSelect = viewModel::selectTarget,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.compareEnabled,
                    onClick = { viewModel.setCompareEnabled(!state.compareEnabled) },
                    label = { Text(stringResource(R.string.history_compare)) }
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        viewModel.exportToCsv { path ->
                            Toast.makeText(context, if (path != null) csvSaved else csvFailed, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = state.hasData
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.cd_export_csv))
                }
            }
            if (state.compareEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.compareBaseInfo?.let { info ->
                        CurrencySelector(
                            label = stringResource(R.string.converter_from),
                            currency = info,
                            onSelect = viewModel::selectCompareBase,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.compareTargetInfo?.let { info ->
                        CurrencySelector(
                            label = stringResource(R.string.converter_to),
                            currency = info,
                            onSelect = viewModel::selectCompareTarget,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Hızlı para çifti seçici
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pairs) { (base, target) ->
                    FilterChip(
                        selected = state.baseCurrency == base && state.targetCurrency == target,
                        onClick = { viewModel.selectPair(base, target) },
                        label = { Text("${getFlagEmoji(base)} $base / ${getFlagEmoji(target)} $target") }
                    )
                }
            }

            // Zaman aralığı seçici
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.timeRange == range,
                        onClick = { viewModel.selectTimeRange(range) },
                        label = { Text(stringResource(range.labelRes), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Moving averages need enough history and are not drawn on the normalized comparison chart.
            val sma50 = remember(state.data) { movingAverage(state.data, 50) }
            val sma200 = remember(state.data) { movingAverage(state.data, 200) }
            if (!state.compareEnabled && (sma50.isNotEmpty() || sma200.isNotEmpty())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sma50.isNotEmpty()) {
                        SmaChip("SMA 50", Sma50Color, showSma50) { showSma50 = it }
                    }
                    if (sma200.isNotEmpty()) {
                        SmaChip("SMA 200", Sma200Color, showSma200) { showSma200 = it }
                    }
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.hasData) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val chartDescription = stringResource(
                            R.string.history_chart_description,
                            "${state.baseCurrency}/${state.targetCurrency}",
                            "%.4f".format(state.data.minOf { it.rate }),
                            "%.4f".format(state.data.maxOf { it.rate }),
                            "%.4f".format(state.data.last().rate)
                        )
                        RateChart(
                            description = chartDescription,
                            data = state.data,
                            compareData = if (state.compareEnabled) state.compareData else emptyList(),
                            lineColor = MaterialTheme.colorScheme.primary,
                            compareColor = CompareLineColor,
                            overlays = if (state.compareEnabled) emptyList() else listOfNotNull(
                                if (showSma50 && sma50.isNotEmpty()) sma50 to Sma50Color else null,
                                if (showSma200 && sma200.isNotEmpty()) sma200 to Sma200Color else null
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isWide) 340.dp else 250.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        if (state.compareEnabled && state.compareData.isNotEmpty()) {
                            ComparisonSummary(
                                primaryLabel = "${state.baseCurrency}/${state.targetCurrency}",
                                primary = state.data,
                                primaryColor = MaterialTheme.colorScheme.primary,
                                compareLabel = "${state.compareBase}/${state.compareTarget}",
                                compare = state.compareData,
                                compareColor = CompareLineColor
                            )
                        } else {
                            RateSummary(state.data)
                            RateTimingCard(state.data)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (state.error != null) {
                            stringResource(R.string.dashboard_error_fetch)
                        } else {
                            stringResource(R.string.history_no_data)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    TextButton(onClick = viewModel::reload) {
                        Text(stringResource(R.string.converter_retry))
                    }
                }
            }
    }
}

private val Sma50Color = Color(0xFF9CCC65)
private val Sma200Color = Color(0xFFBA68C8)

@Composable
private fun SmaChip(label: String, color: Color, selected: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label) },
        leadingIcon = { Box(Modifier.size(10.dp).background(color, CircleShape)) }
    )
}

@Composable
private fun RateChart(
    description: String,
    data: List<HistoricalRate>,
    compareData: List<HistoricalRate>,
    lineColor: Color,
    compareColor: Color,
    modifier: Modifier = Modifier,
    overlays: List<Pair<List<HistoricalRate>, Color>> = emptyList()
) {
    if (data.isEmpty()) return

    val comparing = compareData.isNotEmpty()
    fun normalized(series: List<HistoricalRate>): List<HistoricalRate> {
        val start = series.firstOrNull()?.rate ?: return emptyList()
        if (!comparing || start <= 0.0) return series
        return series.map { it.copy(rate = (it.rate / start - 1.0) * 100.0) }
    }
    val primary = normalized(data)
    val secondary = normalized(compareData)
    val allPoints = primary + secondary
    val firstDate = allPoints.minOf { it.date }
    val lastDate = allPoints.maxOf { it.date }
    val timeSpan = (lastDate - firstDate).coerceAtLeast(1L)
    val minRate = allPoints.minOf { it.rate }
    val maxRate = allPoints.maxOf { it.rate }
    val rateRange = (maxRate - minRate).takeIf { it > 0 } ?: 0.0001
    // Narrow percent ranges need a second decimal or neighbouring grid labels read the same.
    fun axisLabel(value: Double) = when {
        !comparing -> formatYLabel(value)
        rateRange < 2.0 -> "%.2f%%".format(value)
        else -> "%.1f%%".format(value)
    }

    val textMeasurer    = rememberTextMeasurer()
    val labelColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor       = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val labelStyle      = TextStyle(fontSize = 9.sp, color = labelColor)
    val scrubStyle      = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    val spanDays = if (data.size >= 2) {
        ((data.last().date - data.first().date) / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
    } else {
        1L
    }
    val datePattern = if (spanDays > 400) "MMM yy" else "dd MMM"
    val dateSdf         = remember(datePattern) { SimpleDateFormat(datePattern, Locale.getDefault()) }
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }

    fun seriesPoints(
        series: List<HistoricalRate>,
        chartL: Float,
        chartT: Float,
        chartW: Float,
        chartH: Float
    ): List<Offset> {
        if (series.isEmpty()) return emptyList()
        return series.map { point ->
                Offset(
                    chartL + ((point.date - firstDate).toDouble() / timeSpan * chartW).toFloat(),
                    chartT + ((maxRate - point.rate) / rateRange * chartH).toFloat()
                )
        }
    }

    Canvas(
        modifier = modifier
            // Screen readers cannot follow a drawn line, so describe the range and the latest value.
            .semantics { contentDescription = description }
            .pointerInput(data, compareData) {
                fun pick(x: Float): Int {
                    val left = textMeasurer.measure(axisLabel(maxRate), labelStyle).size.width + 10f
                    val frac = ((x - left) / (size.width - left).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val date = firstDate + frac * timeSpan
                    return data.indices.minBy { kotlin.math.abs(data[it].date - date) }
                }
                detectTapGestures { selectedIndex = pick(it.x) }
            }
            .pointerInput(data, compareData) {
                fun pick(x: Float): Int {
                    val left = textMeasurer.measure(axisLabel(maxRate), labelStyle).size.width + 10f
                    val frac = ((x - left) / (size.width - left).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val date = firstDate + frac * timeSpan
                    return data.indices.minBy { kotlin.math.abs(data[it].date - date) }
                }
                detectDragGestures(
                    onDragStart = { selectedIndex = pick(it.x) },
                    onDrag = { change, _ -> selectedIndex = pick(change.position.x) }
                )
            }
    ) {
        val sampleLabel   = axisLabel(maxRate)
        val sampleMeasure = textMeasurer.measure(sampleLabel, labelStyle)
        val yLabelW       = sampleMeasure.size.width.toFloat() + 10f
        val xLabelH       = sampleMeasure.size.height.toFloat() + 6f

        val chartL = yLabelW
        val chartR = size.width
        val chartT = 18f
        val chartB = size.height - xLabelH
        val chartW = (chartR - chartL).coerceAtLeast(1f)
        val chartH = (chartB - chartT).coerceAtLeast(1f)

        val steps = 5
        repeat(steps + 1) { i ->
            val frac       = i.toFloat() / steps
            val rateValue  = maxRate - frac * rateRange
            val y          = chartT + frac * chartH
            drawLine(gridColor, Offset(chartL, y), Offset(chartR, y), strokeWidth = 1f)
            val label   = axisLabel(rateValue)
            val measure = textMeasurer.measure(label, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text          = label,
                topLeft       = Offset(0f, y - measure.size.height / 2f),
                style         = labelStyle
            )
        }

        val points = seriesPoints(primary, chartL, chartT, chartW, chartH)
        val comparePoints = seriesPoints(secondary, chartL, chartT, chartW, chartH)

        fun drawSeries(pts: List<Offset>, color: Color, fill: Boolean) {
            if (pts.size >= 2) {
                if (fill) {
                    val fillPath = Path().apply {
                        moveTo(pts.first().x, chartB)
                        pts.forEach { lineTo(it.x, it.y) }
                        lineTo(pts.last().x, chartB)
                        close()
                    }
                    drawPath(
                        path  = fillPath,
                        brush = Brush.verticalGradient(
                            colors   = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                            startY   = chartT,
                            endY     = chartB
                        )
                    )
                }
                val linePath = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(linePath, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        drawSeries(points, lineColor, fill = true)
        drawSeries(comparePoints, compareColor, fill = false)
        if (!comparing) {
            overlays.forEach { (series, color) ->
                drawSeries(seriesPoints(series, chartL, chartT, chartW, chartH), color, fill = false)
            }
        }

        val idx = selectedIndex
        if (idx != null && idx in data.indices) {
            val x = points[idx].x
            drawLine(lineColor.copy(alpha = 0.5f), Offset(x, chartT), Offset(x, chartB), strokeWidth = 2f)
            val point = data[idx]
            val label = "${dateSdf.format(Date(point.date))}  ${axisLabel(primary[idx].rate)}"
            val measure = textMeasurer.measure(label, scrubStyle)
            val lx = (x - measure.size.width / 2f).coerceIn(chartL, (chartR - measure.size.width).coerceAtLeast(chartL))
            drawText(textMeasurer = textMeasurer, text = label, topLeft = Offset(lx, 0f), style = scrubStyle)
        }

        if (data.size <= 15) {
            points.forEach { p ->
                drawCircle(Color.White, radius = 5f, center = p)
                drawCircle(lineColor, radius = 3.5f, center = p)
            }
        }

        val labelCount = minOf(4, data.size)
        repeat(labelCount) { i ->
            val di     = if (labelCount == 1) 0 else (i * (data.size - 1)) / (labelCount - 1)
            val x = points[di].x
            val dateStr = dateSdf.format(Date(data[di].date))
            val measure = textMeasurer.measure(dateStr, labelStyle)
            val labelX  = (x - measure.size.width / 2f).coerceIn(chartL, (chartR - measure.size.width).coerceAtLeast(chartL))
            drawText(
                textMeasurer = textMeasurer,
                text          = dateStr,
                topLeft       = Offset(labelX, chartB + 4f),
                style         = labelStyle
            )
        }
    }
}

private fun formatYLabel(rate: Double): String = when {
    rate >= 10_000 -> "%.0f".format(rate)
    rate >= 1_000  -> "%.1f".format(rate)
    rate >= 100    -> "%.2f".format(rate)
    rate >= 10     -> "%.3f".format(rate)
    else           -> "%.4f".format(rate)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RateSummary(data: List<HistoricalRate>) {
    if (data.isEmpty()) return
    val min  = data.minOf { it.rate }
    val max  = data.maxOf { it.rate }
    val last = data.last()
    val first = data.first()
    val change = if (first.rate != 0.0) (last.rate - first.rate) / first.rate * 100.0 else null

    // Wraps to a second line instead of letting long labels and values run into each other.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryItem(stringResource(R.string.history_low), "%.4f".format(min))
        SummaryItem(stringResource(R.string.history_high), "%.4f".format(max))
        SummaryItem(stringResource(R.string.history_current), "%.4f".format(last.rate))
        if (change != null) {
            SummaryItem(stringResource(R.string.history_change), "%+.2f%%".format(change))
        }
    }
}

/** Where the latest rate sits in the loaded period: percentile, versus average, and a low-to-high bar. */
@Composable
private fun RateTimingCard(data: List<HistoricalRate>) {
    val timing = remember(data) { rateTiming(data) } ?: return
    Spacer(Modifier.height(16.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_timing_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = { timing.positionInRange },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.history_low), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.history_high), style = MaterialTheme.typography.labelSmall)
            }
            Text(
                stringResource(R.string.history_timing_percentile, timing.percentile),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.history_timing_vs_average, "%+.1f%%".format(timing.vsAveragePercent)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private val CompareLineColor = Color(0xFFFFB300)

/** Percent change per series, colored like the lines, since the chart is normalized when comparing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComparisonSummary(
    primaryLabel: String,
    primary: List<HistoricalRate>,
    primaryColor: Color,
    compareLabel: String,
    compare: List<HistoricalRate>,
    compareColor: Color
) {
    fun change(series: List<HistoricalRate>): String {
        val first = series.firstOrNull()?.rate ?: return "—"
        val last = series.last().rate
        return if (first != 0.0) "%+.2f%%".format((last - first) / first * 100.0) else "—"
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LegendItem(primaryLabel, change(primary), primaryColor)
        LegendItem(compareLabel, change(compare), compareColor)
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
