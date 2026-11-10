package com.hayhak.currencyconverter.domain.usecase

import com.hayhak.currencyconverter.domain.model.CurrencyPairDefaults
import com.hayhak.currencyconverter.domain.model.currencyPairDefaults
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Live [CurrencyPairDefaults] from the user's Rates base and ordered favorites. */
class ObserveCurrencyDefaults @Inject constructor(private val userPrefs: UserPreferencesRepository) {
    operator fun invoke(): Flow<CurrencyPairDefaults> =
        combine(userPrefs.lastDashboardBase, userPrefs.favoriteOrder) { base, favorites ->
            currencyPairDefaults(base, favorites)
        }
}
