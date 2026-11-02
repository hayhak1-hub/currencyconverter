package com.hayhak.currencyconverter.ui.portfolio

import com.hayhak.currencyconverter.data.store.model.PortfolioEntry
import com.hayhak.currencyconverter.domain.model.quotePerUnit

/** One holding priced at today's rates. Amounts in [PortfolioEntry.costCode] unless marked otherwise. */
data class HoldingRow(
    val entry: PortfolioEntry,
    val cost: Double,
    val value: Double,
    val pnl: Double,
    val pnlPercent: Double?
)

data class PortfolioSummary(
    val rows: List<HoldingRow>,
    /** Totals converted into the chosen display currency. */
    val displayCode: String,
    val totalCost: Double,
    val totalValue: Double,
    /** Entries that could not be priced because a rate is missing; left out of the totals. */
    val unpriced: Int
) {
    val pnl: Double get() = totalValue - totalCost
    val pnlPercent: Double? get() = if (totalCost > 0.0) pnl / totalCost * 100.0 else null
}

fun holdingRow(entry: PortfolioEntry, usdRates: Map<String, Double>): HoldingRow? {
    val unitValue = quotePerUnit(entry.code, entry.costCode, usdRates) ?: return null
    val cost = entry.amount * entry.costPerUnit
    val value = entry.amount * unitValue
    if (!cost.isFinite() || !value.isFinite()) return null
    val pnl = value - cost
    return HoldingRow(entry, cost, value, pnl, if (cost > 0.0) pnl / cost * 100.0 else null)
}

fun portfolioSummary(
    entries: List<PortfolioEntry>,
    usdRates: Map<String, Double>,
    displayCode: String
): PortfolioSummary {
    val rows = ArrayList<HoldingRow>()
    var totalCost = 0.0
    var totalValue = 0.0
    var unpriced = 0
    for (entry in entries) {
        val row = holdingRow(entry, usdRates)
        val toDisplay = quotePerUnit(entry.costCode, displayCode, usdRates)
        if (row == null || toDisplay == null) {
            unpriced++
            continue
        }
        rows += row
        totalCost += row.cost * toDisplay
        totalValue += row.value * toDisplay
    }
    return PortfolioSummary(rows, displayCode, totalCost, totalValue, unpriced)
}
