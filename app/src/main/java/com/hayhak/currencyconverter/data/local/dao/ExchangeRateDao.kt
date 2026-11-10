package com.hayhak.currencyconverter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hayhak.currencyconverter.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLatest(rates: List<com.hayhak.currencyconverter.data.local.entity.LatestRateEntity>)

    @Query("DELETE FROM latest_rates WHERE baseCurrency = :base")
    suspend fun deleteLatest(base: String)

    @androidx.room.Transaction
    suspend fun replaceLatest(base: String, rates: List<ExchangeRateEntity>) {
        if (rates.isEmpty()) return
        deleteLatest(base)
        insertLatest(rates.map {
            com.hayhak.currencyconverter.data.local.entity.LatestRateEntity(it.baseCurrency, it.targetCurrency, it.rate, it.timestamp)
        })
        replaceHistory(base, rates.minOf { it.timestamp }, rates.maxOf { it.timestamp }, rates)
    }

    @androidx.room.Transaction
    suspend fun replaceHistory(base: String, from: Long, to: Long, rates: List<ExchangeRateEntity>) {
        rates.map { it.targetCurrency }.distinct().forEach { target ->
            deletePairInRange(base, target, from, to)
        }
        insertRates(rates)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Query("""
        SELECT 0 AS id, baseCurrency, targetCurrency, rate, timestamp FROM latest_rates
        WHERE baseCurrency = :base
    """)
    fun getLatestRates(base: String = "USD"): Flow<List<ExchangeRateEntity>>

    @Query("""
        SELECT * FROM exchange_rates
        WHERE baseCurrency = :base AND targetCurrency = :target
        AND timestamp >= :sinceTimestamp
        ORDER BY timestamp ASC
    """)
    fun getHistoricalRates(
        base: String,
        target: String,
        sinceTimestamp: Long
    ): Flow<List<ExchangeRateEntity>>

    @Query("SELECT MAX(timestamp) FROM latest_rates WHERE baseCurrency = :base")
    suspend fun getLastUpdateTime(base: String = "USD"): Long?

    @Query("""
        SELECT * FROM exchange_rates e
        WHERE e.baseCurrency = :base
        AND timestamp = (
            SELECT MAX(timestamp) FROM exchange_rates
            WHERE baseCurrency = :base AND targetCurrency = e.targetCurrency AND timestamp < :beforeTimestamp
        )
    """)
    suspend fun getRatesBefore(base: String, beforeTimestamp: Long): List<ExchangeRateEntity>

    @Query("""
        DELETE FROM exchange_rates
        WHERE baseCurrency = :base
          AND targetCurrency = :target
          AND timestamp >= :fromTimestamp
          AND timestamp <= :toTimestamp
    """)
    suspend fun deletePairInRange(
        base: String,
        target: String,
        fromTimestamp: Long,
        toTimestamp: Long
    )

    /** Only prune very old history (keep ~5.5 years). Never wipe recent chart data. */
    @Query("DELETE FROM exchange_rates WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
