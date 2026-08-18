package com.hayhak.currencyconverter.domain.repository

import com.hayhak.currencyconverter.domain.model.ExchangeRate
import com.hayhak.currencyconverter.domain.model.HistoricalRate
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun getLatestRates(base: String = "USD"): Flow<ExchangeRate?>
    fun getHistoricalRates(base: String, target: String, daysBack: Int): Flow<List<HistoricalRate>>
    suspend fun refreshRates(base: String = "USD")
    suspend fun refreshHistoricalRates(base: String, target: String, daysBack: Int)
    suspend fun getLastUpdateTime(base: String = "USD"): Long?
    suspend fun getRatesBefore(base: String, beforeTimestamp: Long): ExchangeRate?
}
