package com.hayhak.currencyconverter.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatePromptPolicyTest {
    private val day = 24L * 60 * 60 * 1000
    private val installed = 1_000_000L

    @Test fun hiddenUntilThreeDaysAfterInstall() {
        val state = RatePromptState(firstSeenAt = installed)
        assertFalse(RatePromptPolicy.shouldShow(installed + 3 * day - 1, state))
        assertTrue(RatePromptPolicy.shouldShow(installed + 3 * day, state))
    }

    @Test fun laterKeepsTheThreeDayCadence() {
        val shown = installed + 3 * day
        val state = RatePromptState(firstSeenAt = installed, lastShownAt = shown, accepted = false)
        assertFalse(RatePromptPolicy.shouldShow(shown + 3 * day - 1, state))
        assertTrue(RatePromptPolicy.shouldShow(shown + 3 * day, state))
    }

    @Test fun okDropsTheCadenceToOnceAWeek() {
        val shown = installed + 3 * day
        val state = RatePromptState(firstSeenAt = installed, lastShownAt = shown, accepted = true)
        assertFalse(RatePromptPolicy.shouldShow(shown + 3 * day, state))
        assertFalse(RatePromptPolicy.shouldShow(shown + 7 * day - 1, state))
        assertTrue(RatePromptPolicy.shouldShow(shown + 7 * day, state))
    }

    @Test fun missingInstallTimeNeverShows() {
        assertFalse(RatePromptPolicy.shouldShow(installed + 30 * day, RatePromptState()))
    }
}
