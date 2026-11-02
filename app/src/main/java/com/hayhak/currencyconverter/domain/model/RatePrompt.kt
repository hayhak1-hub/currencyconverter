package com.hayhak.currencyconverter.domain.model

data class RatePromptState(
    val firstSeenAt: Long = 0L,
    val lastShownAt: Long = 0L,
    val accepted: Boolean = false
)

object RatePromptPolicy {
    const val EVERY_THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000
    const val EVERY_WEEK_MS = 7L * 24 * 60 * 60 * 1000

    fun intervalMillis(accepted: Boolean): Long =
        if (accepted) EVERY_WEEK_MS else EVERY_THREE_DAYS_MS

    fun shouldShow(now: Long, state: RatePromptState): Boolean {
        if (state.firstSeenAt <= 0L || now < state.firstSeenAt) return false
        val anchor = if (state.lastShownAt > 0L) state.lastShownAt else state.firstSeenAt
        return now - anchor >= intervalMillis(state.accepted)
    }
}
