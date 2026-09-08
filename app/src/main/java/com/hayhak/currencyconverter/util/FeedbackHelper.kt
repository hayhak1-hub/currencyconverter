package com.hayhak.currencyconverter.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.hayhak.currencyconverter.BuildConfig
import com.hayhak.currencyconverter.R

object FeedbackHelper {
    fun openFeedbackEmail(context: Context) {
        val to = context.getString(R.string.settings_feedback_email)
        val subject = context.getString(
            R.string.settings_feedback_subject,
            BuildConfig.VERSION_NAME
        )
        val body = context.getString(
            R.string.settings_feedback_body,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT
        )
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to")).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.settings_feedback_chooser)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_feedback_no_email),
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_feedback_no_email),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
