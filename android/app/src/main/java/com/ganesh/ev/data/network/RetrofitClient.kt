package com.ganesh.ev.data.network

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val BASE_URL = com.ganesh.ev.BuildConfig.BASE_URL

    @Volatile
    private var authToken: String = ""
    @Volatile
    private var refreshToken: String = ""

    // Invoked when the Authenticator rotates tokens, so the new values can be
    // persisted (e.g. to DataStore) and survive a process restart (CV-1).
    @Volatile
    private var tokenPersister: ((access: String, refresh: String?) -> Unit)? = null

    fun setTokenPersister(persister: (access: String, refresh: String?) -> Unit) {
        synchronized(this) {
            tokenPersister = persister
        }
    }

    fun setAuthToken(token: String) {
        synchronized(this) {
            authToken = token
        }
    }

    fun getAuthToken(): String = authToken

    fun setRefreshToken(token: String) {
        synchronized(this) {
            refreshToken = token
        }
    }

    fun clearAuthTokens() {
        synchronized(this) {
            authToken = ""
            refreshToken = ""
        }
    }

    // Secondary client just for refresh calls to avoid interceptor recursion
    private val refreshClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val refreshApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val authInterceptor = Interceptor { chain ->
            val request =
                    chain.request()
                            .newBuilder()
                            .apply {
                                if (authToken.isNotEmpty()) {
                                    addHeader("Authorization", "Bearer $authToken")
                                }
                            }
                            .build()
            chain.proceed(request)
        }

        val authenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                if (refreshToken.isEmpty()) return null

                // Stop after 3 attempts to avoid infinite loops if server is down or token is invalid
                if (responseCount(response) >= 3) {
                    SessionEvents.notifySessionExpired()
                    return null
                }

                return synchronized(this) {
                    runBlocking {
                        try {
                            val refreshResponse = refreshApiService.refreshToken(
                                com.ganesh.ev.data.model.TokenRefreshRequest(refreshToken)
                            )
                            
                            if (refreshResponse.isSuccessful) {
                                val newData = refreshResponse.body()?.data
                                val newAccess = newData?.token
                                if (newAccess != null) {
                                    authToken = newAccess
                                    // Capture a rotated refresh token if the backend issued one.
                                    val newRefresh = newData.refreshToken
                                    if (!newRefresh.isNullOrEmpty()) {
                                        refreshToken = newRefresh
                                    }
                                    // Persist the rotated tokens so they survive a restart (CV-1).
                                    tokenPersister?.invoke(newAccess, newRefresh)
                                    response.request.newBuilder()
                                        .header("Authorization", "Bearer $newAccess")
                                        .build()
                                } else {
                                    // Refresh succeeded but returned no token — unrecoverable.
                                    SessionEvents.notifySessionExpired()
                                    null
                                }
                            } else {
                                // Refresh was rejected — the session has truly expired.
                                SessionEvents.notifySessionExpired()
                                null
                            }
                        } catch (e: Exception) {
                            // Network error (likely offline) — don't force a logout.
                            null
                        }
                    }
                }
            }
        }

        return OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .authenticator(authenticator)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var currResponse: Response? = response.priorResponse
        while (currResponse != null) {
            result++
            currResponse = currResponse.priorResponse
        }
        return result
    }

    private val retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
