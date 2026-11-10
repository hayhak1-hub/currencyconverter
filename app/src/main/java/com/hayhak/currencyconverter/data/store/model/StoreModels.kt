package com.hayhak.currencyconverter.data.store.model

/**
 * Models persisted as JSON by the stores in [com.hayhak.currencyconverter.data.store].
 * Keep the field names stable (see proguard-rules.pro); new fields need defaults so old files still load.
 */

/** One alert that actually fired and produced a notification. */
data class AlarmEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val baseCode: String,
    val targetCode: String,
    val rate: Double,
    /** Threshold for rate alerts, percent for percent alerts. */
    val trigger: Double,
    val isAbove: Boolean,
    val percent: Boolean = false,
    val firedAt: Long
)

/** A holding the user recorded by hand: how much of a currency and what they paid per unit. */
data class PortfolioEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val code: String,
    val amount: Double,
    /** Price paid for one unit of [code], in [costCode]. */
    val costPerUnit: Double,
    val costCode: String,
    val boughtAt: Long,
    val note: String = ""
)

data class TripExpense(
    val id: String = java.util.UUID.randomUUID().toString(),
    /** Amount in the trip's local currency. */
    val amount: Double,
    val label: String = "",
    val at: Long
)

data class Trip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    /** Currency spent abroad. */
    val localCode: String,
    /** Currency the budget is counted in. */
    val homeCode: String,
    /** Total budget in [homeCode]. */
    val budget: Double,
    val expenses: List<TripExpense> = emptyList(),
    val createdAt: Long
)
