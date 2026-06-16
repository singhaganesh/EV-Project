package com.ganesh.ev

import android.app.Application
import com.ganesh.ev.data.local.StationCache
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.notifications.Notifications
import com.ganesh.ev.data.repository.UserPreferencesRepository
import com.ganesh.ev.service.ChargingManager
import com.ganesh.ev.util.AppAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Wires the network layer's token rotation back into persistent storage so a
 * session refreshed by the OkHttp [Authenticator] survives a process restart
 * (CV-1). [RetrofitClient] is a Context-less singleton, so it cannot touch
 * DataStore directly — instead we hand it a persister callback here.
 */
class EvApplication : Application() {

    // Process-scoped IO scope for fire-and-forget token persistence.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        AppAnalytics.init(this)
        AppAnalytics.appOpen()

        Notifications.createChannels(this)
        ChargingManager.init(this)
        StationCache.init(this)

        val prefs = UserPreferencesRepository(applicationContext)

        RetrofitClient.setTokenPersister { access, refresh ->
            appScope.launch {
                prefs.saveAuthToken(access)
                if (!refresh.isNullOrEmpty()) {
                    prefs.saveRefreshToken(refresh)
                }
            }
        }
    }
}
