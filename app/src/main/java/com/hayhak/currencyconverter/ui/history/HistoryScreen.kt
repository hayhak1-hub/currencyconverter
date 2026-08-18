package com.hayhak.currencyconverter.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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
import com.hayhak.currencyconverter.ui.components.CurrencySelector
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val csvSaved = stringResource(R.string.history_csv_saved)
    val csvFailed = stringResource(R.string.history_csv_failed)

    val PAIRS = listOf(
        "USD" to "TRY",
        "EUR" to "TRY",
        "GBP" to "TRY",
        "USD" to "EUR",
        "USD" to "GBP",
        "EUR" to "GBP"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.exportToCsv { path ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (path != null) csvSaved else csvFailed
                            )
                        }
                    }
                }) {
                    Icon(Icons.Default.Download, contentDescription = stringResource(R.string.cd_export_csv))
                }
            }

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

            // Hızlı para çifti seçici
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PAIRS) { (base, target) ->
                    FilterChip(
                        selected = state.baseCurrency == base && state.targetCurrency == target,
                        onClick = { viewModel.selectPair(base, target) },
                        label = { Text("$base/$target") }
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
                        RateChart(
                            data = state.data,
                            lineColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        RateSummary(state.data)
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.history_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RateChart(
    data: List<HistoricalRate>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer    = rememberTextMeasurer()
    val labelColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor       = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val labelStyle      = TextStyle(fontSize = 9.sp, color = labelColor)
    val spanDays = if (data.size >= 2) {
        ((data.last().date - data.first().date) / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
    } else {
        1L
    }
    val datePattern = if (spanDays > 400) "MMM yy" else "dd MMM"
    val dateSdf         = remember(datePattern) { SimpleDateFormat(datePattern, Locale.getDefault()) }

    Canvas(modifier = modifier) {
        val minRate   = data.minOf { it.rate }
        val maxRate   = data.maxOf { it.rate }
        val rateRange = (maxRate - minRate).takeIf { it > 0 } ?: (maxRate * 0.01).coerceAtLeast(0.0001)

        val sampleLabel   = formatYLabel(maxRate)
        val sampleMeasure = textMeasurer.measure(sampleLabel, labelStyle)
        val yLabelW       = sampleMeasure.size.width.toFloat() + 10f
        val xLabelH       = sampleMeasure.size.height.toFloat() + 6f

        val chartL = yLabelW
        val chartR = size.width
        val chartT = 4f
        val chartB = size.height - xLabelH
        val chartW = (chartR - chartL).coerceAtLeast(1f)
        val chartH = (chartB - chartT).coerceAtLeast(1f)

        val steps = 5
        repeat(steps + 1) { i ->
            val frac       = i.toFloat() / steps
            val rateValue  = maxRate - frac * rateRange
            val y          = chartT + frac * chartH

            drawLine(gridColor, Offset(chartL, y), Offset(chartR, y), strokeWidth = 1f)

            val label   = formatYLabel(rateValue)
            val measure = textMeasurer.measure(label, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text          = label,
                topLeft       = Offset(0f, y - measure.size.height / 2f),
                style         = labelStyle
            )
        }

        val points = if (data.size == 1) {
            listOf(Offset(chartL + chartW / 2f, chartT + chartH / 2f))
        } else {
            data.mapIndexed { i, point ->
                val x = chartL + (i.toFloat() / (data.size - 1)) * chartW
                val y = chartT + ((maxRate - point.rate) / rateRange * chartH).toFloat()
                Offset(x, y)
            }
        }

        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, chartB)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, chartB)
                close()
            }
            drawPath(
                path  = fillPath,
                brush = Brush.verticalGradient(
                    colors   = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                    startY   = chartT,
                    endY     = chartB
                )
            )

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                linePath, lineColor,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        if (data.size <= 15) {
            points.forEach { p ->
                drawCircle(Color.White, radius = 5f, center = p)
                drawCircle(lineColor, radius = 3.5f, center = p)
            }
        }

        val labelCount = minOf(4, data.size)
        repeat(labelCount) { i ->
            val idx     = if (labelCount == 1) 0 else (i * (data.size - 1)) / (labelCount - 1)
            val x       = if (data.size == 1) {
                chartL + chartW / 2f
            } else {
                chartL + (idx.toFloat() / (data.size - 1)) * chartW
            }
            val dateStr = dateSdf.format(Date(data[idx].date))
            val measure = textMeasurer.measure(dateStr, labelStyle)
            val labelX  = (x - measure.size.width / 2f).coerceIn(chartL, chartR - measure.size.width)
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

@Composable
private fun RateSummary(data: List<HistoricalRate>) {
    if (data.isEmpty()) return
    val min  = data.minOf { it.rate }
    val max  = data.maxOf { it.rate }
    val last = data.last()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryItem(stringResource(R.string.history_low), "%.4f".format(min))
        SummaryItem(stringResource(R.string.history_high), "%.4f".format(max))
        SummaryItem(stringResource(R.string.history_current), "%.4f".format(last.rate))
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
