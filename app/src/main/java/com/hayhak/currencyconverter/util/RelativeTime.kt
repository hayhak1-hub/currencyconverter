package com.hayhak.currencyconverter.util

import android.content.Context
import com.hayhak.currencyconverter.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelativeTime(context: Context, ts: Long): String {
    val minutes = ((System.currentTimeMillis() - ts) / 60_000).toInt()
    return when {
        minutes < 1 -> context.getString(R.string.dashboard_just_now)
        minutes < 60 -> context.resources.getQuantityString(R.plurals.minutes_ago, minutes, minutes)
        minutes < 24 * 60 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
        else -> SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(ts))
    }
}

fun isRateStale(ts: Long?, maxAgeHours: Long = 6): Boolean {
    if (ts == null) return true
    return System.currentTimeMillis() - ts > maxAgeHours * 60 * 60 * 1000
}
