package com.hayhak.currencyconverter.domain.usecase

import com.hayhak.currencyconverter.domain.model.ExchangeRate
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    operator fun invoke(base: String = "USD"): Flow<ExchangeRate?> =
        repository.getLatestRates(base)
}
