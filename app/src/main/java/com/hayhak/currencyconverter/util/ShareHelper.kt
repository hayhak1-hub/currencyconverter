package com.hayhak.currencyconverter.util

import android.content.Context
import android.content.Intent

object ShareHelper {
    fun shareText(context: Context, title: String, text: String) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                title
            )
        )
    }
}
