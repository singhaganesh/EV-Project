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
        @SerializedName("createdAt") val createdAt: String?,
        @SerializedName("paymentStatus") val paymentStatus: String? = null
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
        @SerializedName("totalCost") val totalCost: Double?,
        @SerializedName("status") val status: String?,
        @SerializedName("paymentStatus") val paymentStatus: String?,
        @SerializedName("razorpayOrderId") val razorpayOrderId: String?
)

data class SimpleChargingSession(
    val id: Long,
    val session: ChargingSession?,
    val razorpayOrderId: String?,
    val totalCost: Double?,
    val razorpayKeyId: String?
)

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
data class FirebaseLoginRequest(@SerializedName("idToken") val idToken: String)

data class TokenRefreshRequest(val refreshToken: String)

data class DeviceTokenRequest(
        @SerializedName("deviceToken") val deviceToken: String,
        val platform: String = "android"
)

data class UpdateProfileRequest(
        val name: String,
        val email: String?
)

data class Vehicle(
        val id: Long,
        val make: String?,
        val model: String?,
        @SerializedName("batteryKwh") val batteryKwh: Double?,
        @SerializedName("connectorType") val connectorType: String?
)

data class Review(
        val id: Long,
        @SerializedName("userName") val userName: String?,
        val rating: Int,
        val comment: String?,
        @SerializedName("createdAt") val createdAt: String?
)

data class ReviewSummary(
        val reviews: List<Review> = emptyList(),
        val average: Double = 0.0,
        val count: Long = 0,
        @SerializedName("canReview") val canReview: Boolean = false,
        @SerializedName("alreadyReviewed") val alreadyReviewed: Boolean = false
)

data class ReviewRequest(
        val rating: Int,
        val comment: String?
)

data class BookingTemplate(
        val id: Long,
        @SerializedName("stationId") val stationId: Long,
        @SerializedName("vehicleId") val vehicleId: Long?,
        @SerializedName("connectorType") val connectorType: String,
        @SerializedName("vehicleType") val vehicleType: String,
        @SerializedName("timeOfDay") val timeOfDay: String,   // "HH:mm[:ss]"
        @SerializedName("daysOfWeek") val daysOfWeek: String,  // CSV "MON,TUE"
        val active: Boolean
)

data class BookingTemplateRequest(
        @SerializedName("stationId") val stationId: Long,
        @SerializedName("vehicleId") val vehicleId: Long?,
        @SerializedName("connectorType") val connectorType: String,
        @SerializedName("vehicleType") val vehicleType: String,
        @SerializedName("timeOfDay") val timeOfDay: String,   // "HH:mm"
        @SerializedName("daysOfWeek") val daysOfWeek: String,
        val active: Boolean? = null
)

data class VehicleRequest(
        val make: String,
        val model: String,
        @SerializedName("batteryKwh") val batteryKwh: Double?,
        @SerializedName("connectorType") val connectorType: String?
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
    val pricePerKwh: Double,
    // Backend sets this true on the final frame (battery full / overtime) so the
    // app can tear down telemetry and advance to the payment screen.
    val completed: Boolean = false
)

data class PaginatedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val numberOfElements: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)

data class DispensaryAnalyticsDTO(
    val dispensaryId: Long,
    val totalSessions: Long,
    val totalEnergyKwh: Double,
    val avgDurationMinutes: Double,
    val peakHours: List<PeakHourDTO>
)

data class PeakHourDTO(
    val hour: Int,
    val sessionCount: Long
)
