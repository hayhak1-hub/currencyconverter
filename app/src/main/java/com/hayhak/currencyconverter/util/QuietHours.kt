package com.hayhak.currencyconverter.util

import com.hayhak.currencyconverter.domain.model.QuietHours
import java.util.Calendar

fun isQuietNow(hours: QuietHours, now: Calendar = Calendar.getInstance()): Boolean {
    if (!hours.enabled) return false
    val start = hours.startMinute.coerceIn(0, 24 * 60 - 1)
    val end = hours.endMinute.coerceIn(0, 24 * 60)
    val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    return if (start == end) {
        false
    } else if (start < end) {
        minute in start until end
    } else {
        minute >= start || minute < end
    }
}

fun formatMinuteOfDay(minute: Int): String {
    val clamped = minute.coerceIn(0, 24 * 60 - 1)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}
