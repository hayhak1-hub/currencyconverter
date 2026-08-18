package com.hayhak.currencyconverter.domain.usecase

import com.hayhak.currencyconverter.domain.model.HistoricalRate
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoricalRatesUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    operator fun invoke(
        base: String,
        target: String,
        daysBack: Int = 7
    ): Flow<List<HistoricalRate>> =
        repository.getHistoricalRates(base, target, daysBack)
}
