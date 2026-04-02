package com.ganesh.stationfinder.data.network

import com.ganesh.stationfinder.data.model.OCMStation
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenChargeMapApi {
    
    @GET("v3/poi")
    suspend fun getNearbyStations(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("distance") distance: Double,
        @Query("distanceunit") unit: String = "KM",
        @Query("maxresults") maxResults: Int = 20,
        @Query("compact") compact: Boolean = true,
        @Query("verbose") verbose: Boolean = false
    ): List<OCMStation>
}
