package com.hayhak.currencyconverter.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.hayhak.currencyconverter.R

object PlayStoreHelper {
    fun openListing(context: Context) {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_rate_open_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
