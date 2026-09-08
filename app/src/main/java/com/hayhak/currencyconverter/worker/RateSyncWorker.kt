package com.hayhak.currencyconverter.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
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
            repository.refreshRates("USD")
            checkAlarms()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun checkAlarms() {
        val alarms = userPrefs.alarms.first()
        if (alarms.isEmpty()) return

        val notificationHelper = NotificationHelper(context)
        val bases = alarms.map { it.baseCode }.distinct()
        val cooldownMs = TimeUnit.HOURS.toMillis(12)

        for (base in bases) {
            try {
                repository.refreshRates(base)
                val rateData = repository.getLatestRates(base).first() ?: continue

                val baseAlarms = alarms.filter { it.baseCode == base && it.isEnabled }
                for (alarm in baseAlarms) {
                    val currentRate = rateData.rates[alarm.targetCode] ?: continue
                    val triggered = if (alarm.isAbove) {
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

                    val msgRes = if (alarm.isAbove) R.string.notif_alarm_above else R.string.notif_alarm_below
                    notificationHelper.showRateNotification(
                        title = context.getString(
                            R.string.notif_alarm_title,
                            alarm.baseCode,
                            alarm.targetCode
                        ),
                        message = context.getString(msgRes, alarm.threshold.toString(), currentRate.toString())
                    )
                    if (alarm.repeating) {
                        userPrefs.updateAlarm(alarm.copy(lastFiredAt = System.currentTimeMillis()))
                    } else {
                        userPrefs.updateAlarm(alarm.copy(isEnabled = false, lastFiredAt = System.currentTimeMillis()))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val WORK_NAME = "RateSyncWorker"
    }
}
