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
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Query("""
        SELECT * FROM exchange_rates
        WHERE baseCurrency = :base
        AND timestamp = (SELECT MAX(timestamp) FROM exchange_rates WHERE baseCurrency = :base)
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

    @Query("SELECT MAX(timestamp) FROM exchange_rates WHERE baseCurrency = :base")
    suspend fun getLastUpdateTime(base: String = "USD"): Long?

    @Query("""
        SELECT * FROM exchange_rates
        WHERE baseCurrency = :base
        AND timestamp = (
            SELECT MAX(timestamp) FROM exchange_rates
            WHERE baseCurrency = :base AND timestamp < :beforeTimestamp
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
