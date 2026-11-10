package com.hayhak.currencyconverter.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.BASE_CURRENCY
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.util.NotificationHelper
import com.hayhak.currencyconverter.util.isQuietNow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class RateSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            repository.refreshRates(BASE_CURRENCY)
            checkAlarms()
            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun checkAlarms() {
        val alarms = userPrefs.alarms.first()
        if (alarms.isEmpty()) return
        if (isQuietNow(userPrefs.quietHours.first())) return

        val notificationHelper = NotificationHelper(context)
        val bases = alarms.map { it.baseCode }.distinct()
        val cooldownMs = TimeUnit.HOURS.toMillis(12)

        for (base in bases) {
            try {
                // BASE_CURRENCY was already refreshed in doWork().
                if (base != BASE_CURRENCY) repository.refreshRates(base)
                val rateData = repository.getLatestRates(base).first() ?: continue
                val previous = repository.getRatesBefore(
                    base,
                    System.currentTimeMillis() - TimeUnit.HOURS.toMillis(20)
                )?.rates

                val baseAlarms = alarms.filter { it.baseCode == base && it.isEnabled }
                for (alarm in baseAlarms) {
                    val currentRate = rateData.rates[alarm.targetCode] ?: continue
                    val dayChange = previous?.get(alarm.targetCode)?.takeIf { it != 0.0 }?.let { prev ->
                        (currentRate - prev) / prev * 100.0
                    }
                    val triggered = if (alarm.isPercent) {
                        val change = dayChange ?: continue
                        if (alarm.isAbove) change >= alarm.percent else change <= -alarm.percent
                    } else if (alarm.isAbove) {
                        currentRate >= alarm.threshold
                    } else {
                        currentRate <= alarm.threshold
                    }
                    if (!triggered) continue
                    if (alarm.repeating && alarm.lastFiredAt > 0 &&
                        System.currentTimeMillis() - alarm.lastFiredAt < cooldownMs
                    ) {
                        continue
                    }

                    val message = if (alarm.isPercent) {
                        val res = if (alarm.isAbove) R.string.notif_alarm_percent_up else R.string.notif_alarm_percent_down
                        context.getString(
                            res,
                            String.format(Locale.getDefault(), "%.1f", alarm.percent),
                            String.format(Locale.getDefault(), "%.1f", dayChange ?: 0.0)
                        )
                    } else {
                        val msgRes = if (alarm.isAbove) R.string.notif_alarm_above else R.string.notif_alarm_below
                        context.getString(msgRes, alarm.threshold.toString(), currentRate.toString())
                    }
                    val posted = notificationHelper.showRateNotification(
                        title = context.getString(
                            R.string.notif_alarm_title,
                            alarm.baseCode,
                            alarm.targetCode
                        ),
                        message = message,
                        from = alarm.baseCode,
                        to = alarm.targetCode
                    )
                    if (!posted) continue
                    if (alarm.repeating) {
                        userPrefs.updateAlarm(alarm.copy(lastFiredAt = System.currentTimeMillis()))
                    } else {
                        userPrefs.updateAlarm(alarm.copy(isEnabled = false, lastFiredAt = System.currentTimeMillis()))
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(WORK_NAME, "Alarm check failed for $base", e)
            }
        }
    }

    companion object {
        const val WORK_NAME = "RateSyncWorker"
    }
}
