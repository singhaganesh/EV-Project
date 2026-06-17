package com.ganesh.ev.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ganesh.ev.data.model.DeviceTokenRequest
import com.ganesh.ev.data.network.RetrofitClient
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Durable FCM device-token registration (A2).
 *
 * Replaces a fire-and-forget coroutine so a registration that fails (offline,
 * transient 5xx) is retried by WorkManager with backoff. If the user isn't
 * logged in yet there's nothing to register, so it succeeds as a no-op and is
 * re-enqueued at the next login instead of retrying forever.
 */
class DeviceTokenWorker(
        context: Context,
        params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (RetrofitClient.getAuthToken().isEmpty()) return@withContext Result.success()
        try {
            val token = Tasks.await(FirebaseMessaging.getInstance().token)
            val response = RetrofitClient.apiService.registerDeviceToken(DeviceTokenRequest(token))
            if (response.isSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
