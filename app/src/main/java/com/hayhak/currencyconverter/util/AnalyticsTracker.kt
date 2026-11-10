package com.hayhak.currencyconverter.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Injectable entry point so view models do not need a Context just to log events. */
@Singleton
class AnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun logConversion(from: String, to: String) {
        AnalyticsHelper.logConversion(context, from, to)
    }
}
