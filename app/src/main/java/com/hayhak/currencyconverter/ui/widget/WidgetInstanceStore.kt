package com.hayhak.currencyconverter.ui.widget

import android.content.Context

object WidgetInstanceStore {
    private const val PREFS = "widget_instances"

    fun save(context: Context, appWidgetId: Int, base: String, codes: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("base_$appWidgetId", base)
            .putString("codes_$appWidgetId", codes.joinToString(","))
            .commit()
    }

    fun load(context: Context, appWidgetId: Int): Pair<String, List<String>>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val base = prefs.getString("base_$appWidgetId", null) ?: return null
        val codes = prefs.getString("codes_$appWidgetId", null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return base to codes
    }
}
