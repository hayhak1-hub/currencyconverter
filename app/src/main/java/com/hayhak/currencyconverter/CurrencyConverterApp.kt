package com.hayhak.currencyconverter

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hayhak.currencyconverter.domain.repository.UserPreferencesRepository
import com.hayhak.currencyconverter.util.ShortcutPublisher
import com.hayhak.currencyconverter.worker.DigestScheduler
import com.hayhak.currencyconverter.worker.RateSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CurrencyConverterApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPrefs: UserPreferencesRepository

    @Inject
    lateinit var shortcuts: ShortcutPublisher

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Crash reports from release builds only; debug runs would just add noise.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        RateSyncScheduler.schedule(this)
        appScope.launch {
            combine(userPrefs.savedPairs, userPrefs.favoriteOrder) { pairs, favorites ->
                pairs to favorites
            }.collect { (pairs, favorites) -> shortcuts.publish(pairs, favorites) }
        }
        appScope.launch {
            // Usage statistics are opt-in; the manifest keeps collection off until this runs.
            userPrefs.analyticsEnabled.collect { enabled ->
                FirebaseAnalytics.getInstance(this@CurrencyConverterApp).setAnalyticsCollectionEnabled(enabled)
            }
        }
        appScope.launch {
            userPrefs.morningDigest.collect { enabled ->
                DigestScheduler.schedule(this@CurrencyConverterApp, enabled)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
