package com.hayhak.currencyconverter.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hayhak.currencyconverter.R
import com.hayhak.currencyconverter.domain.model.BASE_CURRENCY
import com.hayhak.currencyconverter.domain.model.quotePerUnit
import com.hayhak.currencyconverter.domain.repository.ExchangeRateRepository
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.util.NotificationHelper
import com.hayhak.currencyconverter.util.ShortcutPublisher
import com.hayhak.currencyconverter.util.isQuietNow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class MorningDigestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ExchangeRateRepository,
    private val userPrefs: UserPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!userPrefs.morningDigest.first()) return Result.success()
        if (isQuietNow(userPrefs.quietHours.first())) return Result.success()
        return try {
            repository.refreshRates(BASE_CURRENCY)
            val today = repository.getLatestRates(BASE_CURRENCY).first()?.rates ?: return Result.success()
            val yesterday = repository.getRatesBefore(
                BASE_CURRENCY,
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(20)
            )?.rates
            val savedPairs = userPrefs.savedPairs.first()
            val favoriteOrder = userPrefs.favoriteOrder.first()
            val pairs = savedPairs.ifEmpty {
                ShortcutPublisher.pairsFrom(savedPairs, favoriteOrder)
            }.take(4)
            val lines = pairs.mapNotNull { (from, to) ->
                val rate = quotePerUnit(from, to, today) ?: return@mapNotNull null
                val prev = yesterday?.let { quotePerUnit(from, to, it) }
                val change = if (prev != null && prev != 0.0) (rate - prev) / prev * 100.0 else null
                val changeText = change?.let { " (%+.1f%%)".format(Locale.getDefault(), it) }.orEmpty()
                "$from → $to  ${"%.4f".format(Locale.getDefault(), rate)}$changeText"
            }
            if (lines.isEmpty()) return Result.success()
            NotificationHelper(applicationContext).showDigest(
                title = applicationContext.getString(R.string.digest_title),
                message = lines.joinToString("\n")
            )
            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "MorningDigestWorker"
    }
}
