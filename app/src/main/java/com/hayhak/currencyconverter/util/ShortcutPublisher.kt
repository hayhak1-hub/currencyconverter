package com.hayhak.currencyconverter.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.hayhak.currencyconverter.MainActivity
import com.hayhak.currencyconverter.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutPublisher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun publish(savedPairs: List<Pair<String, String>>, favoriteOrder: List<String>) {
        val shortcuts = pairsFrom(savedPairs, favoriteOrder).mapIndexed { index, (from, to) ->
            ShortcutInfoCompat.Builder(context, "$from-$to")
                .setShortLabel("$from/$to")
                .setLongLabel("$from → $to")
                .setRank(index)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_NAV, MainActivity.NAV_CONVERTER)
                        putExtra(MainActivity.EXTRA_FROM, from)
                        putExtra(MainActivity.EXTRA_TARGET, to)
                    }
                )
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    companion object {
        fun pairsFrom(saved: List<Pair<String, String>> = emptyList(), order: List<String> = emptyList()): List<Pair<String, String>> {
            val fromFavorites = order.filter { it != "USD" }.map { "USD" to it }
            val fallback = listOf("USD" to "EUR", "EUR" to "GBP", "USD" to "JPY")
            return (saved + fromFavorites + fallback).distinct().take(3)
        }
    }
}
