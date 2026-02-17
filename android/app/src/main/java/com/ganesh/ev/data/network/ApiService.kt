package com.ganesh.ev.data.network

import com.ganesh.ev.data.model.ApiResponse
import com.ganesh.ev.data.model.AuthResponse
import com.ganesh.ev.data.model.Booking
import com.ganesh.ev.data.model.BookingRequest
import com.ganesh.ev.data.model.ChargerSlot
import com.ganesh.ev.data.model.ChargingSession
import com.ganesh.ev.data.model.CompleteProfileRequest
import com.ganesh.ev.data.model.LivePowerData
import com.ganesh.ev.data.model.Payment
import com.ganesh.ev.data.model.PaymentIntentResponse
import com.ganesh.ev.data.model.StartChargingRequest
import com.ganesh.ev.data.model.Station
import com.ganesh.ev.data.model.StationWithScore
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

        // Auth APIs
        @POST("api/auth/send-otp")
        suspend fun sendOtp(
                @Query("mobileNumber") mobileNumber: String
        ): Response<ApiResponse<Map<String, String>>>

        @POST("api/auth/validate-otp")
        suspend fun validateOtp(
                @Query("mobileNumber") mobileNumber: String,
                @Query("otp") otp: String
        ): Response<AuthResponse>

        @POST("api/auth/complete-profile")
        suspend fun completeProfile(@Body request: CompleteProfileRequest): Response<AuthResponse>

        // Station APIs
        @GET("api/stations") suspend fun getAllStations(): Response<List<Station>>

        @GET("api/stations/{id}")
        suspend fun getStationById(@Path("id") id: Long): Response<ApiResponse<Station>>

        // Slot APIs
        @GET("api/slots") suspend fun getAllSlots(): Response<ApiResponse<List<ChargerSlot>>>

        @GET("api/slots/{id}") suspend fun getSlotById(@Path("id") id: Long): Response<ChargerSlot>

        @GET("api/slots/station/{stationId}")
        suspend fun getSlotsByStation(
                @Path("stationId") stationId: Long
        ): Response<ApiResponse<List<ChargerSlot>>>

        @GET("api/slots/station/{stationId}/available")
        suspend fun getAvailableSlots(
                @Path("stationId") stationId: Long
        ): Response<ApiResponse<List<ChargerSlot>>>

        // Booking APIs
        @GET("api/bookings") suspend fun getAllBookings(): Response<ApiResponse<List<Booking>>>

        @GET("api/bookings/user/{userId}")
        suspend fun getUserBookings(
                @Path("userId") userId: Long
        ): Response<ApiResponse<List<Booking>>>

        @POST("api/bookings")
        suspend fun createBooking(@Body request: BookingRequest): Response<ApiResponse<Booking>>

        @PUT("api/bookings/{bookingId}/cancel")
        suspend fun cancelBooking(@Path("bookingId") bookingId: Long): Response<ApiResponse<Void>>

        // Charging Session APIs
        @POST("api/charging/start")
        suspend fun startCharging(
                @Body request: StartChargingRequest
        ): Response<ApiResponse<ChargingSession>>

        @POST("api/charging/stop/{sessionId}")
        suspend fun stopCharging(
                @Path("sessionId") sessionId: Long
        ): Response<ApiResponse<ChargingSession>>

        @GET("api/charging/session/{sessionId}")
        suspend fun getSession(
                @Path("sessionId") sessionId: Long
        ): Response<ApiResponse<ChargingSession>>

        @GET("api/charging/booking/{bookingId}")
        suspend fun getSessionByBooking(
                @Path("bookingId") bookingId: Long
        ): Response<ApiResponse<ChargingSession>>

        @GET("api/charging/user/{userId}")
        suspend fun getUserChargingHistory(
                @Path("userId") userId: Long
        ): Response<ApiResponse<List<ChargingSession>>>

        // Payment APIs
        @POST("api/payments/create-intent/{bookingId}")
        suspend fun createPaymentIntent(
                @Path("bookingId") bookingId: Long
        ): Response<ApiResponse<PaymentIntentResponse>>

        @GET("api/payments/booking/{bookingId}")
        suspend fun getPaymentStatus(
                @Path("bookingId") bookingId: Long
        ): Response<ApiResponse<Payment>>

        // Paper Methodology APIs
        @GET("api/stations/nearby")
        suspend fun getNearbyStations(
                @Query("lat") latitude: Double,
                @Query("lng") longitude: Double,
                @Query("radius") radius: Double = 10.0
        ): Response<ApiResponse<List<StationWithScore>>>

        @GET("api/iot/stations/{stationId}/live-power")
        suspend fun getStationLivePower(
                @Path("stationId") stationId: Long
        ): Response<ApiResponse<LivePowerData>>
}
