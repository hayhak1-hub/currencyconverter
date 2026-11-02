package com.hayhak.currencyconverter.ui.trips

import com.hayhak.currencyconverter.data.store.model.Trip
import com.hayhak.currencyconverter.domain.model.quotePerUnit

data class TripStatus(
    /** Total spent, converted to the trip's home currency. */
    val spentHome: Double,
    val remainingHome: Double,
    /** 0..1 share of the budget used; above 1 means overspent. */
    val usedFraction: Double,
    /** Expenses that could not be converted because a rate is missing. */
    val unconverted: Int
) {
    val overBudget: Boolean get() = remainingHome < 0
}

fun tripStatus(trip: Trip, usdRates: Map<String, Double>): TripStatus {
    val perUnit = quotePerUnit(trip.localCode, trip.homeCode, usdRates)
    val spent = if (perUnit == null) 0.0 else trip.expenses.sumOf { it.amount } * perUnit
    val fraction = if (trip.budget > 0.0) spent / trip.budget else 0.0
    return TripStatus(
        spentHome = spent,
        remainingHome = trip.budget - spent,
        usedFraction = fraction,
        unconverted = if (perUnit == null) trip.expenses.size else 0
    )
}
