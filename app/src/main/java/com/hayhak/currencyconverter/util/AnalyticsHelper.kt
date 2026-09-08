package com.hayhak.currencyconverter.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/** Privacy-safe Analytics events: no amounts, no personal identifiers. */
object AnalyticsHelper {

    fun logScreen(context: Context, screenName: String) {
        log(
            context,
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
        )
    }

    fun logLanguageSelected(context: Context, languageTag: String) {
        log(
            context,
            "language_selected",
            Bundle().apply { putString("language_tag", languageTag.ifBlank { "system" }) }
        )
    }

    fun logConversion(context: Context, from: String, to: String) {
        log(
            context,
            "currency_converted",
            Bundle().apply {
                putString("from_currency", from)
                putString("to_currency", to)
            }
        )
    }

    private fun log(context: Context, name: String, params: Bundle = Bundle.EMPTY) {
        runCatching {
            FirebaseAnalytics.getInstance(context.applicationContext).logEvent(name, params)
        }
    }
}
