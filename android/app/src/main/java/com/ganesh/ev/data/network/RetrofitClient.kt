package com.ganesh.ev.data.network

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // IMPORTANT: Change this IP to your computer's IP address
    // For Emulator: Use "http://10.0.2.2:8080/" (special IP = laptop's localhost)
    // For Physical Device: Use your laptop's IPv4 address (e.g., "http://192.168.0.248:8080/")
    // AWS Live Backend:
    private const val BASE_URL = "http://10.136.227.45:8080/"

    private var authToken: String = ""

    fun setAuthToken(token: String) {
        authToken = token
    }

    fun clearAuthToken() {
        authToken = ""
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

        return OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
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
