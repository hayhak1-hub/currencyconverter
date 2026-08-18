package com.hayhak.currencyconverter.util

import android.content.Context
import android.util.Log
import com.hayhak.currencyconverter.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await

data class PlayUpdateInfo(
    val currentVersionCode: Int,
    val availableVersionCode: Int,
) {
    val isOutdated: Boolean get() = availableVersionCode > currentVersionCode
}

enum class UpdateCheckStatus {
    AVAILABLE,
    UP_TO_DATE,
    UNAVAILABLE,
}

data class PlayUpdateCheckResult(
    val status: UpdateCheckStatus,
    val info: PlayUpdateInfo? = null,
)

object PlayUpdateChecker {
    private const val TAG = "PlayUpdateChecker"

    suspend fun checkDetailed(context: Context): PlayUpdateCheckResult {
        return try {
            val manager = AppUpdateManagerFactory.create(context.applicationContext)
            val info = manager.appUpdateInfo.await()
            val availability = info.updateAvailability()
            val availableCode = info.availableVersionCode()
            val currentCode = BuildConfig.VERSION_CODE

            Log.d(TAG, "availability=$availability current=$currentCode available=$availableCode")

            if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                val typeAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ||
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                if (typeAllowed && availableCode > currentCode) {
                    return PlayUpdateCheckResult(
                        status = UpdateCheckStatus.AVAILABLE,
                        info = PlayUpdateInfo(currentCode, availableCode),
                    )
                }
            }

            PlayUpdateCheckResult(status = UpdateCheckStatus.UP_TO_DATE)
        } catch (e: Exception) {
            Log.d(TAG, "Update check failed (expected for sideload/debug): ${e.message}")
            PlayUpdateCheckResult(status = UpdateCheckStatus.UNAVAILABLE)
        }
    }
}
