package com.ganesh.stationfinder.data.network

import com.ganesh.stationfinder.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.openchargemap.io/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "StationFinder-Android-App")
                .header("X-API-Key", BuildConfig.OCM_API_KEY)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1)) 
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val api: OpenChargeMapApi = retrofit.create(OpenChargeMapApi::class.java)
}
