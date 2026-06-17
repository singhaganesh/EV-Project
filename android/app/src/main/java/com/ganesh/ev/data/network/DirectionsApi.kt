package com.ganesh.ev.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Minimal client for the Google Directions REST API (E2).
 *
 * Used directly from the device with the app's Maps key so route planning needs
 * no server-side Google integration. The `destination` may be a free-text place
 * name — Directions geocodes it — so no separate geocoding step is required.
 */
interface DirectionsApi {

    @GET("maps/api/directions/json")
    suspend fun getDirections(
            @Query("origin") origin: String,        // "lat,lng"
            @Query("destination") destination: String, // place name or "lat,lng"
            @Query("key") key: String
    ): Response<DirectionsResponse>
}

data class DirectionsResponse(
        val routes: List<DirectionsRoute> = emptyList(),
        val status: String? = null
)

data class DirectionsRoute(
        @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline?,
        val legs: List<DirectionsLeg> = emptyList()
)

data class OverviewPolyline(val points: String?)

data class DirectionsLeg(
        val distance: TextValue?,
        val duration: TextValue?,
        @SerializedName("end_address") val endAddress: String?
)

data class TextValue(val text: String?)

object DirectionsClient {
    val api: DirectionsApi by lazy {
        Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DirectionsApi::class.java)
    }
}
