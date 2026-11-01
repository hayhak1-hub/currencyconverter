package com.hayhak.currencyconverter.data.repository

import com.hayhak.currencyconverter.data.local.dao.ExchangeRateDao
import com.hayhak.currencyconverter.data.local.entity.ExchangeRateEntity
import com.hayhak.currencyconverter.data.remote.api.ExchangeRateApi
import com.hayhak.currencyconverter.data.remote.api.LiveExchangeRateApi
import com.hayhak.currencyconverter.domain.model.ExchangeRate
import com.hayhak.currencyconverter.domain.model.HistoricalRate
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ExchangeRateRepositoryImpl @Inject constructor(
    private val liveApi: LiveExchangeRateApi,
    private val frankfurterApi: ExchangeRateApi,
    private val dao: ExchangeRateDao
) : ExchangeRateRepository {

    private fun utcDayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun getLatestRates(base: String): Flow<ExchangeRate?> {
        return dao.getLatestRates(base).map { entities ->
            if (entities.isEmpty()) return@map null
            val timestamp = entities.maxOf { it.timestamp }
            val rates = entities.associate { it.targetCurrency to it.rate }
            ExchangeRate(base, rates, timestamp)
        }
    }

    override fun getHistoricalRates(
        base: String,
        target: String,
        daysBack: Int
    ): Flow<List<HistoricalRate>> {
        val sinceTimestamp = System.currentTimeMillis() -
            TimeUnit.DAYS.toMillis(daysBack.toLong())
        return dao.getHistoricalRates(base, target, sinceTimestamp).map { entities ->
            entities
                .groupBy { utcDayKey().format(Date(it.timestamp)) }
                .entries
                .sortedBy { it.key }
                .map { (_, group) ->
                    val latest = group.maxBy { it.timestamp }
                    HistoricalRate(latest.timestamp, latest.rate)
                }
        }
    }

    override suspend fun refreshHistoricalRates(base: String, target: String, daysBack: Int) {
        try {
            val sdf = utcDayKey()
            val fetchDays = when {
                daysBack <= 1 -> 10
                daysBack <= 7 -> daysBack.coerceAtLeast(7)
                else -> daysBack
            }
            val group = when {
                daysBack > 400 -> "month"
                daysBack > 90 -> "week"
                else -> null
            }

            val endMs = System.currentTimeMillis()
            val startMs = endMs - TimeUnit.DAYS.toMillis(fetchDays.toLong())
            val chunkDays = when (group) {
                "month" -> 400
                "week" -> 180
                else -> 120
            }

            val entities = fetchHistoricalInChunks(
                base = base,
                target = target,
                startMs = startMs,
                endMs = endMs,
                chunkDays = chunkDays,
                group = group,
                dateFormat = sdf
            )

            if (entities.isNotEmpty()) {
                dao.replaceHistory(base, entities.minOf { it.timestamp }, entities.maxOf { it.timestamp }, entities)
            }
        } catch (e: Exception) {
            android.util.Log.e("ExchangeRateRepo", "Failed to refresh history $base/$target ($daysBack d)", e)
            throw e
        }
    }

    private suspend fun fetchHistoricalInChunks(
        base: String,
        target: String,
        startMs: Long,
        endMs: Long,
        chunkDays: Int,
        group: String?,
        dateFormat: SimpleDateFormat
    ): List<ExchangeRateEntity> {
        val chunkMs = TimeUnit.DAYS.toMillis(chunkDays.toLong())
        val entities = mutableListOf<ExchangeRateEntity>()
        var chunkStart = startMs

        while (chunkStart <= endMs) {
            val chunkEnd = minOf(chunkStart + chunkMs, endMs)
            val rows = frankfurterApi.getHistoricalRates(
                base = base,
                quotes = target,
                from = dateFormat.format(Date(chunkStart)),
                to = dateFormat.format(Date(chunkEnd)),
                group = group
            )
            rows.forEach { row ->
                if (row.quote.equals(base, ignoreCase = true)) return@forEach
                val date = dateFormat.parse(row.date) ?: return@forEach
                entities += ExchangeRateEntity(
                    baseCurrency = base,
                    targetCurrency = row.quote,
                    rate = row.rate,
                    timestamp = date.time
                )
            }
            if (chunkEnd >= endMs) break
            chunkStart = chunkEnd + TimeUnit.DAYS.toMillis(1)
        }

        return entities
            .groupBy { "${it.timestamp}_${it.targetCurrency}" }
            .values
            .map { group -> group.last() }
    }

    override suspend fun refreshRates(base: String) {
        try {
            refreshLiveRates(base)
        } catch (e: Exception) {
            android.util.Log.w("ExchangeRateRepo", "Live API failed for $base, using Frankfurter", e)
            if (e is kotlinx.coroutines.CancellationException) throw e
            refreshFrankfurterRates(base)
        }
    }

    private suspend fun refreshLiveRates(base: String) {
        android.util.Log.d("ExchangeRateRepo", "Refreshing live rates for $base")
        val response = liveApi.getLatestRates(base)
        if (response.result != "success" || response.rates.isEmpty()) {
            throw IllegalStateException("Live API returned no data for $base")
        }

        val timestamp = response.timeLastUpdateUnix
            .takeIf { it > 0 }
            ?.times(1000L)
            ?: System.currentTimeMillis()

        val entities = response.rates
            .filter { !it.key.equals(base, ignoreCase = true) }
            .map { (quote, rate) ->
                ExchangeRateEntity(
                    baseCurrency = base,
                    targetCurrency = quote,
                    rate = rate,
                    timestamp = timestamp
                )
            }

        android.util.Log.d("ExchangeRateRepo", "Live API rows: ${entities.size}")
        dao.replaceLatest(base, entities)
        pruneOldHistory(timestamp)
    }

    private suspend fun refreshFrankfurterRates(base: String) {
        android.util.Log.d("ExchangeRateRepo", "Refreshing Frankfurter rates for $base")
        val rows = frankfurterApi.getLatestRates(base = base)
        android.util.Log.d("ExchangeRateRepo", "Frankfurter rows: ${rows.size}")
        val timestamp = System.currentTimeMillis()
        val entities = rows
            .filter { !it.quote.equals(base, ignoreCase = true) }
            .map { row ->
                ExchangeRateEntity(
                    baseCurrency = base,
                    targetCurrency = row.quote,
                    rate = row.rate,
                    timestamp = utcDayKey().parse(row.date)?.time ?: timestamp
                )
            }
        if (entities.isEmpty()) throw IllegalStateException("No rates returned for $base")
        dao.replaceLatest(base, entities)
        pruneOldHistory(timestamp)
    }

    private suspend fun pruneOldHistory(timestamp: Long) {
        val historyCutoff = timestamp - TimeUnit.DAYS.toMillis(2000)
        dao.deleteOlderThan(historyCutoff)
    }

    override suspend fun getLastUpdateTime(base: String): Long? =
        dao.getLastUpdateTime(base)

    override suspend fun getRatesBefore(base: String, beforeTimestamp: Long): ExchangeRate? {
        val entities = dao.getRatesBefore(base, beforeTimestamp)
        if (entities.isEmpty()) return null
        val ts = entities.maxOf { it.timestamp }
        val rates = entities.associate { it.targetCurrency to it.rate }
        return ExchangeRate(base, rates, ts)
    }
}
