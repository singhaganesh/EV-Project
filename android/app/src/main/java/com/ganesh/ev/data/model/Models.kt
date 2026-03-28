package com.ganesh.ev.data.model

import com.google.gson.annotations.SerializedName

data class User(
        val id: Long,
        @SerializedName("mobileNumber") val mobileNumber: String,
        val name: String?,
        val email: String?,
        @SerializedName("isFirstTimeUser") val isFirstTimeUser: Boolean = true,
        val role: String = "CUSTOMER",
        @SerializedName("createdAt") val createdAt: String?,
        @SerializedName("updatedAt") val updatedAt: String?
)

data class Station(
        val id: Long,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val meta: String?,
        val operatingHours: String? = null,
        val pricePerKwh: Double? = null,
        val truckPricePerKwh: Double? = null,
        val isOpen: Boolean? = null,
        @SerializedName("lastUsedTime") val lastUsedTime: String? = null
)

data class Dispensary(
        val id: Long,
        val name: String,
        val totalPowerKw: Double,
        val acceptsTrucks: Boolean,
        @SerializedName("lastUsedTime") val lastUsedTime: String? = null
)

data class ChargerSlot(
        val id: Long,
        @SerializedName("slotNumber") val slotNumber: String?,
        @SerializedName("slotType") val slotType: SlotType,
        @SerializedName("connectorType") val connectorType: ConnectorType,
        @SerializedName("powerRating") val powerRating: Double,
        val status: SlotStatus,
        val station: Station?,
        val dispensary: Dispensary?
)

enum class SlotType {
        AC,
        DC
}

enum class ConnectorType {
        CCS2,
        TYPE_2
}

enum class SlotStatus {
        AVAILABLE,
        RESERVED,
        BOOKED,
        CHARGING,
        MAINTENANCE,
        OCCUPIED
}

data class Booking(
        val id: Long,
        val user: User?,
        val slot: ChargerSlot?,
        @SerializedName("startTime") val startTime: String?,
        @SerializedName("endTime") val endTime: String?,
        val status: BookingStatus,
        @SerializedName("priceEstimate") val priceEstimate: Double?,
        @SerializedName("vehicleType") val vehicleType: String?,
        @SerializedName("expiresAt") val expiresAt: String?,
        @SerializedName("createdAt") val createdAt: String?
)

enum class BookingStatus {
        PENDING,
        CONFIRMED,
        ONGOING,
        COMPLETED,
        EXPIRED,
        CANCELLED
}

data class ChargingSession(
        val id: Long,
        val booking: Booking?,
        @SerializedName("startTime") val startTime: String,
        @SerializedName("endTime") val endTime: String?,
        @SerializedName("energyKwh") val energyKwh: Double?,
        @SerializedName("totalCost") val totalCost: Double?
)

data class SimpleChargingSession(val id: Long)

data class Payment(
        val id: Long,
        val booking: Booking?,
        val amount: Double,
        val currency: String,
        @SerializedName("stripePaymentIntentId") val stripePaymentIntentId: String?,
        val status: PaymentStatus,
        @SerializedName("paymentMethod") val paymentMethod: String?,
        @SerializedName("createdAt") val createdAt: String?,
        @SerializedName("paidAt") val paidAt: String?,
        @SerializedName("failureReason") val failureReason: String?
)

enum class PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
}

// Request/Response Models
data class OtpRequest(val mobileNumber: String)

data class OtpValidateRequest(val mobileNumber: String, val otp: String)

data class TokenRefreshRequest(val refreshToken: String)

data class CompleteProfileRequest(
        @SerializedName("mobileNumber") val mobileNumber: String,
        val name: String,
        val email: String?,
        val role: String = "CUSTOMER"
)

data class BookingRequest(
        @SerializedName("userId") val userId: Long,
        @SerializedName("stationId") val stationId: Long,
        @SerializedName("connectorType") val connectorType: String,
        @SerializedName("vehicleType") val vehicleType: String,
        @SerializedName("allowTruckSlotFallback") val allowTruckSlotFallback: Boolean = false
)

data class StartChargingRequest(@SerializedName("bookingId") val bookingId: Long)

// API Response wrapper
data class ApiResponse<T>(val success: Boolean, val message: String?, val data: T?)

data class AuthResponse(val success: Boolean, val message: String?, val data: AuthData?)

data class AuthData(
        @SerializedName("isNewUser") val isNewUser: Boolean?,
        val token: String?,
        val refreshToken: String?,
        val user: User?
)

data class PaymentIntentResponse(
        @SerializedName("clientSecret") val clientSecret: String,
        @SerializedName("paymentIntentId") val paymentIntentId: String
)

data class SimulatedSession(
    val bookingId: Long,
    val slotId: Long,
    val stationId: Long,
    val powerKw: Double,
    val energyDispensedKwh: Double,
    val socPercentage: Double,
    val totalCost: Double,
    val minutesRemaining: Double,
    val maxPowerKw: Double,
    val batteryCapacityKwh: Double,
    val pricePerKwh: Double
)
