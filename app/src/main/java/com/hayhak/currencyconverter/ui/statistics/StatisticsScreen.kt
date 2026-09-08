package com.hayhak.currencyconverter.ui.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayhak.currencyconverter.R
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    isWide: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh    = viewModel::refresh,
        modifier     = Modifier.fillMaxSize()
    ) {
        if (state.isLoading && state.stats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@PullToRefreshBox
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isWide) 2 else 1),
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Tarih başlığı ─────────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        stringResource(R.string.stats_today),
                        style    = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color    = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.date.isNotEmpty()) {
                        Text(
                            state.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    if (!state.hasYesterdayData) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                stringResource(R.string.stats_need_yesterday),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }

            // ── Döviz kartları ────────────────────────────────────
            items(state.stats, key = { it.code }) { stat ->
                StatCard(stat, state.quoteCode)
            }

            // Alt boşluk
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(8.dp)) }
        }

        // Hata snackbar
        state.error?.let { err ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) { Text(err) }
        }
    }
}

@Composable
private fun StatCard(stat: CurrencyStat, quoteCode: String) {
    val change = stat.changePercent
    val isUp   = (change ?: 0.0) > 0.0
    val isDown = (change ?: 0.0) < 0.0
    val neutral = change == null || change == 0.0

    val changeColor = when {
        neutral -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        isUp    -> Color(0xFF2E7D32)   // yeşil — TRY'a göre pahalılaştı
        else    -> Color(0xFFC62828)   // kırmızı — TRY'a göre ucuzladı
    }

    val animAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "cardAlpha"
    )

    Card(
        modifier  = Modifier.fillMaxWidth().alpha(animAlpha),
        shape     = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bayrak + kod + isim
            Text(stat.flag, fontSize = 28.sp, modifier = Modifier.padding(end = 14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stat.code,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stat.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            // Kur + değişim
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.4f %s".format(stat.rateToTry, quoteCode),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = when {
                            isUp    -> Icons.Default.ArrowUpward
                            isDown  -> Icons.Default.ArrowDownward
                            else    -> Icons.Default.Remove
                        },
                        contentDescription = null,
                        tint     = changeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = if (change != null) "%+.2f%%".format(change) else "—",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = changeColor
                    )
                }
            }
        }

        // Önceki kur — ince alt çubuk
        if (stat.prevRateToTry != null) {
            HorizontalDivider(
                modifier  = Modifier.padding(horizontal = 18.dp),
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.stats_yesterday_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    "%.4f %s".format(stat.prevRateToTry, quoteCode),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
