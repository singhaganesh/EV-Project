# EV Charging Android App - Architecture Analysis

## Project Overview

The EV Charging Android App is a Jetpack Compose-based mobile application that enables users to find EV charging stations, book charging slots, and monitor charging sessions in real-time. The app communicates with a Spring Boot backend via REST APIs and WebSocket connections.

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin 1.9.x |
| **UI Framework** | Jetpack Compose (BOM 2024.02.00) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 35 |
| **Architecture** | MVVM + Clean Architecture |
| **Networking** | Retrofit 2.9.0 + OkHttp 4.12.0 |
| **State Management** | Kotlin StateFlow + Compose State |
| **Local Storage** | DataStore Preferences |
| **Maps** | Google Maps Compose 4.3.0 |
| **Navigation** | Navigation Compose 2.7.7 |
| **DI** | Manual (ViewModelFactory pattern) |
| **Async** | Kotlin Coroutines 1.7.3 |

---

## Project Structure

```
android/app/src/main/java/com/ganesh/ev/
├── MainActivity.kt           # Entry point & Navigation host
├── data/
│   ├── model/
│   │   ├── Models.kt         # Data classes (User, Station, Booking, etc.)
│   │   ├── LivePowerData.kt  # Real-time power telemetry
│   │   ├── StationMarker.kt  # Lightweight map markers
│   │   ├── StationPin.kt     # Cached pin data
│   │   ├── StationWithScore.kt # Station with proximity scoring
│   │   └── ViewportResponse.kt # Viewport API response
│   ├── network/
│   │   ├── ApiService.kt     # Retrofit API interface
│   │   ├── RetrofitClient.kt # HTTP client singleton
│   │   └── StompClient.kt    # WebSocket STOMP client
│   └── repository/
│       └── UserPreferencesRepository.kt # DataStore wrapper
├── ui/
│   ├── screens/              # Composable screens (13 screens)
│   ├── components/          # Reusable UI components
│   ├── viewmodel/           # ViewModels (5 ViewModels)
│   └── theme/               # Material3 theme customization
└── util/
    ├── DateTimeUtils.kt     # Date/time helpers
    └── LocationHelper.kt    # Location utilities
```

---

## Architecture Pattern: MVVM with Clean Architecture Layers

### Layer 1: Data Layer
- **Models**: Kotlin data classes with `@SerializedName` for JSON mapping
- **Network**: Retrofit for REST APIs + OkHttp for WebSocket (STOMP)
- **Repository**: DataStore for persistent preferences

### Layer 2: UI Layer  
- **ViewModels**: StateFlow-based state management
- **Screens**: Composable functions with unidirectional data flow
- **Theme**: Custom "Clay" theme with pastel colors

### Data Flow
```
User Action → ViewModel → Repository → API → Backend
                ↑                              │
                └────── StateFlow <────────────┘
```

---

## Core Features

### 1. Authentication Flow
- **OTP-based login** via mobile number
- **Profile completion** for new users
- **Token-based session** management with JWT
- **Auto-login** on app restart via stored token

**Screens**: LoginScreen, OnboardingScreen, SplashScreen

### 2. Station Discovery
- **Google Maps integration** with custom markers
- **Nearby stations** with distance-based scoring
- **Viewport-based loading** for performance
- **Pin caching** to prevent flickering

**Screens**: HomeScreen, StationDetailScreen

### 3. Slot Booking
- **Real-time availability** display
- **Connector type selection** (CCS2, Type-2)
- **Vehicle type** specification (CAR/TRUCK)
- **Truck slot fallback** option

**Screens**: SlotBookingScreen, BookingConfirmationScreen

### 4. Charging Sessions
- **Real-time telemetry** via WebSocket (STOMP)
- **Live metrics**: SoC, Power, Voltage, Current, Temperature
- **Cost tracking** with rate per kWh
- **Session control** (start/stop)

**Screens**: ChargingScreen, ChargingHistoryScreen

### 5. Booking Management
- **Active bookings** list with status
- **Booking details** with QR/start button
- **Cancellation** support
- **History** with completed sessions

**Screens**: MyBookingsScreen, BookingDetailScreen

---

## Key Data Models

### User
```kotlin
data class User(
    val id: Long,
    val mobileNumber: String,
    val name: String?,
    val email: String?,
    val isFirstTimeUser: Boolean = true,
    val role: String = "CUSTOMER",
    val createdAt: String?,
    val updatedAt: String?
)
```

### Station
```kotlin
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
    val isOpen: Boolean? = null
)
```

### ChargerSlot
```kotlin
data class ChargerSlot(
    val id: Long,
    val slotNumber: String?,
    val slotType: SlotType,       // AC or DC
    val connectorType: ConnectorType, // CCS2 or TYPE_2
    val powerRating: Double,       // kW
    val status: SlotStatus,       // AVAILABLE, BOOKED, CHARGING, etc.
    val station: Station?,
    val dispensary: Dispensary?
)
```

### Booking
```kotlin
data class Booking(
    val id: Long,
    val user: User?,
    val slot: ChargerSlot?,
    val startTime: String?,
    val endTime: String?,
    val status: BookingStatus,    // PENDING, CONFIRMED, ONGOING, COMPLETED, etc.
    val priceEstimate: Double?,
    val vehicleType: String?,
    val expiresAt: String?,
    val createdAt: String?
)
```

### SimulatedSession (Real-time Telemetry)
```kotlin
data class SimulatedSession(
    val bookingId: Long,
    val slotId: Long,
    val stationId: Long,
    val powerKw: Double,
    val energyDispensedKwh: Double,
    val socPercentage: Double,
    val voltageV: Double,
    val currentA: Double,
    val connectorTempC: Double,
    val totalCost: Double,
    val minutesRemaining: Double,
    val maxPowerKw: Double,
    val batteryCapacityKwh: Double,
    val pricePerKwh: Double
)
```

---

## Navigation Structure

### Routes
| Route | Screen | Description |
|-------|--------|-------------|
| `splash` | SplashScreen | App launch with auth check |
| `onboarding` | OnboardingScreen | First-time user guide |
| `login` | LoginScreen | OTP authentication |
| `home` | HomeScreen | Map with nearby stations |
| `station/{stationId}` | StationDetailScreen | Station info & slots |
| `booking/station/{stationId}` | SlotBookingScreen | Slot selection |
| `booking/confirm/...` | BookingConfirmationScreen | Booking summary |
| `bookings/{userId}` | MyBookingsScreen | User's bookings |
| `booking/{bookingId}/user/{userId}` | BookingDetailScreen | Booking actions |
| `charging/booking/{bookingId}` | ChargingScreen | Live charging |
| `history/{userId}` | ChargingHistoryScreen | Past sessions |
| `profile` | ProfileScreen | User profile |

### Bottom Navigation
- **Home** - Map view
- **Bookings** - Active reservations
- **History** - Past charging sessions
- **Profile** - User settings

---

## API Endpoints

### Authentication
- `POST /api/auth/send-otp?mobileNumber={phone}` - Send OTP
- `POST /api/auth/validate-otp?mobileNumber={phone}&otp={code}` - Verify OTP
- `POST /api/auth/complete-profile` - Create new user

### Stations
- `GET /api/stations` - All stations
- `GET /api/stations/{id}` - Station by ID
- `GET /api/stations/nearby?lat=&lng=&radius=` - Nearby stations with scores
- `GET /api/stations/viewport-nearby?neLat=&neLng=&swLat=&swLng=&lat=&lng=` - Viewport + nearby

### Slots
- `GET /api/slots` - All slots
- `GET /api/slots/station/{stationId}` - Station's slots
- `GET /api/slots/station/{stationId}/available` - Available slots

### Bookings
- `GET /api/bookings/user/{userId}` - User's bookings
- `POST /api/bookings` - Create booking
- `PUT /api/bookings/{bookingId}/cancel` - Cancel booking

### Charging
- `POST /api/charging/start` - Start charging session
- `POST /api/charging/stop/{sessionId}` - Stop charging
- `GET /api/charging/session/{sessionId}` - Session details
- `GET /api/charging/booking/{bookingId}` - Session by booking
- `GET /api/charging/user/{userId}` - User's charging history

### Payments
- `POST /api/payments/create-intent/{bookingId}` - Create payment intent
- `GET /api/payments/booking/{bookingId}` - Payment status

### IoT
- `GET /api/iot/stations/{stationId}/live-power` - Live power data

---

## Real-time Communication

### WebSocket (STOMP)
The app uses STOMP over WebSocket for real-time charging telemetry:

**Connection**: `ws://{BASE_URL}/ws/websocket`

**Subscription Topics**:
- `/topic/session/{bookingId}` - Charging session telemetry

**Message Format**:
```json
{
  "bookingId": 123,
  "slotId": 456,
  "stationId": 789,
  "powerKw": 50.0,
  "energyDispensedKwh": 25.5,
  "socPercentage": 75.0,
  "voltageV": 400,
  "currentA": 125,
  "connectorTempC": 35.5,
  "totalCost": 382.50,
  "minutesRemaining": 30,
  "maxPowerKw": 150,
  "batteryCapacityKwh": 75,
  "pricePerKwh": 15
}
```

---

## Theme & Design System

### "Clay" Theme
A custom Material3 theme with warm, earthy pastel colors:

| Color | Hex | Usage |
|-------|-----|-------|
| Primary | `#4ECDC4` | Teal - main actions |
| Secondary | `#FF6B6B` | Coral - accents |
| Tertiary | `#A78BFA` | Lavender - highlights |
| Background | `#F7F3EE` | Warm off-white |
| Surface | `#FFFBF5` | Cream white |

### Slot Status Colors
| Status | Color |
|--------|-------|
| Available | `#B8F0E8` (Mint) |
| Booked | `#FFE0B2` (Peach) |
| Charging | `#BBDEFB` (Blue) |
| Reserved | `#E8E3DB` (Gray) |
| Maintenance | `#FFCDD2` (Red) |
| Occupied | `#FFCCBC` (Orange) |

### Custom Components
- `ClayCard` - Elevated card with rounded corners
- `ClayButton` - Custom styled button
- `ClayTextField` - Text input with validation
- `ClayTopBar` - Custom app bar
- `ClayBottomBar` - Bottom navigation
- `ClayProgressIndicator` - Loading indicator
- `ClayDivider` - Section separator

---

## Build Configuration

### Gradle Dependencies
- AndroidX Core & Lifecycle
- Jetpack Compose (BOM)
- Navigation Compose
- Retrofit + OkHttp
- DataStore Preferences
- Google Maps Compose
- Coil for images

### Build Config Fields
```kotlin
buildConfigField("String", "BASE_URL", "\"${baseUrl}\"")
```

The `BASE_URL` is configured in `local.properties`:
```properties
MAPS_API_KEY=your_maps_api_key
BASE_URL=http://192.168.1.100:8080/
```

---

## State Management

### ViewModel Pattern
Each screen has a dedicated ViewModel with:
- `StateFlow<UiState>` for UI state
- Sealed classes for state variants
- Coroutines for async operations

### Example: AuthViewModel
```kotlin
sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val otp: String, val message: String) : AuthUiState()
    data class OtpValidated(val isNewUser: Boolean, val token: String?, val user: User?) : AuthUiState()
    data class ProfileCompleted(val user: User, val token: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
```

---

## Key Implementation Details

### Camera Position Preservation
The app preserves map camera position across navigation:
```kotlin
fun saveCameraPosition(lat: Double, lng: Double, zoom: Float) {
    savedCameraLat = lat
    savedCameraLng = lng
    savedCameraZoom = zoom
}
```

### Pin Cache for Performance
```kotlin
private val pinCache = mutableMapOf<Long, StationPin>()
```
Prevents markers from flickering during map pan operations.

### Token Management
- Stored in DataStore Preferences
- Added to OkHttp interceptor for automatic header injection
- Cleared on logout

### Shared ViewModel for Booking Flow
The `BookingViewModel` is shared between slot selection and confirmation screens to maintain state across navigation.

---

## Error Handling

### Network Errors
- Caught in ViewModel try-catch blocks
- Displayed via `Error` state in UI
- User-friendly messages shown in ClayCard

### API Error Parsing
```kotlin
val cleanMessage = try {
    val json = org.json.JSONObject(rawErrorBody)
    json.optString("message", rawErrorBody)
} catch (e: Exception) {
    rawErrorBody
}
```

---

## Summary

The EV Charging Android app is a well-structured, modern Android application using:
- **Jetpack Compose** for declarative UI
- **MVVM architecture** with Clean Architecture principles
- **StateFlow** for reactive state management
- **Retrofit + OkHttp** for REST API communication
- **STOMP WebSocket** for real-time telemetry
- **Google Maps** for station discovery
- **DataStore** for local persistence
- **Custom "Clay" theme** for consistent UI design

The app provides a complete EV charging workflow from authentication to charging session monitoring with real-time updates.
