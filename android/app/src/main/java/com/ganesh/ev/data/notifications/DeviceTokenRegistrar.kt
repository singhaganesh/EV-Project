package com.ganesh.ev.data.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Registers this device's FCM token with the backend (CV-11) via WorkManager so
 * a failed registration is retried durably (A2). Enqueued after login and on
 * token rotation; the worker no-ops until the user is authenticated.
 */
object DeviceTokenRegistrar {

    private const val WORK_NAME = "device-token-registration"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<DeviceTokenWorker>()
                .setConstraints(
                        Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                )
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                )
                .build()
        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
