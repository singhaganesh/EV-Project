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

    private var authToken: String = ""
    private var refreshToken: String = ""

    fun setAuthToken(token: String) {
        authToken = token
    }

    fun setRefreshToken(token: String) {
        refreshToken = token
    }

    fun clearAuthTokens() {
        authToken = ""
        refreshToken = ""
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
                                if (newData?.token != null) {
                                    authToken = newData.token
                                    response.request.newBuilder()
                                        .header("Authorization", "Bearer $authToken")
                                        .build()
                                } else null
                            } else null
                        } catch (e: Exception) {
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
