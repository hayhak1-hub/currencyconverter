package com.hayhak.currencyconverter.data.repository

import com.hayhak.currencyconverter.data.local.dao.ExchangeRateDao
import com.hayhak.currencyconverter.data.local.entity.ExchangeRateEntity
import com.hayhak.currencyconverter.data.local.entity.LatestRateEntity
import com.hayhak.currencyconverter.data.remote.api.ExchangeRateApi
import com.hayhak.currencyconverter.data.remote.api.LiveExchangeRateApi
import com.hayhak.currencyconverter.data.remote.dto.FrankfurterRateDto
import com.hayhak.currencyconverter.data.remote.dto.OpenErApiResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExchangeRateRepositoryImplTest {

    private val dao = FakeDao()

    private fun repo(live: LiveExchangeRateApi, frankfurter: ExchangeRateApi = FakeFrankfurter()) =
        ExchangeRateRepositoryImpl(live, frankfurter, dao)

    @Test
    fun `live rates are stored and the base currency is excluded`() = runBlocking {
        val live = FakeLive {
            OpenErApiResponseDto("success", "USD", 1_700_000_000L, mapOf("USD" to 1.0, "EUR" to 0.9))
        }
        repo(live).refreshRates("USD")

        assertEquals(listOf("EUR"), dao.latest.map { it.targetCurrency })
        assertEquals(1_700_000_000_000L, dao.latest.single().timestamp)
    }

    @Test
    fun `falls back to Frankfurter when live API fails`() = runBlocking {
        val live = FakeLive { throw java.io.IOException("down") }
        val frankfurter = FakeFrankfurter(
            latest = listOf(FrankfurterRateDto("2024-01-02", "USD", "TRY", 30.0))
        )
        repo(live, frankfurter).refreshRates("USD")

        assertEquals(listOf("TRY"), dao.latest.map { it.targetCurrency })
    }

    @Test
    fun `cancellation is not swallowed by the fallback`() = runBlocking {
        val live = FakeLive { throw CancellationException("cancelled") }
        val frankfurter = FakeFrankfurter(
            latest = listOf(FrankfurterRateDto("2024-01-02", "USD", "TRY", 30.0))
        )
        try {
            repo(live, frankfurter).refreshRates("USD")
            fail("expected CancellationException")
        } catch (_: CancellationException) {
            assertTrue(dao.latest.isEmpty())
        }
    }

    @Test
    fun `repeated refreshes within the window hit the network once unless forced`() = runBlocking {
        val live = FakeLive {
            OpenErApiResponseDto("success", "USD", 1_700_000_000L, mapOf("EUR" to 0.9))
        }
        val repository = repo(live)

        repository.refreshRates("USD")
        repository.refreshRates("USD")
        assertEquals(1, live.calls)

        repository.refreshRates("USD", force = true)
        assertEquals(2, live.calls)

        repository.refreshRates("EUR")
        assertEquals(3, live.calls)
    }

    private class FakeLive(private val block: () -> OpenErApiResponseDto) : LiveExchangeRateApi {
        var calls = 0
        override suspend fun getLatestRates(base: String): OpenErApiResponseDto {
            calls++
            return block()
        }
    }

    private class FakeFrankfurter(
        private val latest: List<FrankfurterRateDto> = emptyList()
    ) : ExchangeRateApi {
        override suspend fun getLatestRates(base: String) = latest
        override suspend fun getRatesBetween(base: String, from: String, to: String, quotes: String?) = emptyList<FrankfurterRateDto>()
        override suspend fun getHistoricalRates(
            base: String, quotes: String, from: String, to: String, group: String?
        ) = emptyList<FrankfurterRateDto>()
    }

    private class FakeDao : ExchangeRateDao {
        var latest: List<LatestRateEntity> = emptyList()

        override suspend fun insertLatest(rates: List<LatestRateEntity>) { latest = rates }
        override suspend fun deleteLatest(base: String) { latest = emptyList() }
        override suspend fun insertRates(rates: List<ExchangeRateEntity>) = Unit
        override fun getLatestRates(base: String): Flow<List<ExchangeRateEntity>> = emptyFlow()
        override fun getHistoricalRates(base: String, target: String, sinceTimestamp: Long):
            Flow<List<ExchangeRateEntity>> = emptyFlow()
        override suspend fun getLastUpdateTime(base: String): Long? = null
        override suspend fun getRatesBefore(base: String, beforeTimestamp: Long) =
            emptyList<ExchangeRateEntity>()
        override suspend fun deletePairInRange(base: String, target: String, fromTimestamp: Long, toTimestamp: Long) = Unit
        override suspend fun deleteOlderThan(cutoffTimestamp: Long) = Unit
    }
}
