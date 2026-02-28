# EV Charging Station Management System

<p align="center">
  <img src="https://img.shields.io/badge/Spring-Boot-3.3.5-green" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack_Compose-blue" alt="Kotlin">
  <img src="https://img.shields.io/badge/React-19-blueviolet" alt="React">
  <img src="https://img.shields.io/badge/PostgreSQL-Supabase-orange" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

A comprehensive full-stack Electric Vehicle (EV) Charging Station Management Platform featuring mobile applications for EV users, a web portal for station owners and administrators, and a robust backend API with real-time capabilities.

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Features](#features)
5. [Project Structure](#project-structure)
6. [Prerequisites](#prerequisites)
7. [Setup Instructions](#setup-instructions)
8. [API Documentation](#api-documentation)
9. [Database Schema](#database-schema)
10. [User Flows](#user-flows)
11. [Configuration](#configuration)
12. [Running the Application](#running-the-application)
13. [Testing](#testing)
14. [Deployment](#deployment)
15. [Screenshots](#screenshots)
16. [Future Improvements](#future-improvements)
17. [License](#license)

---

## 1. Project Overview

This is a complete EV Charging Station Management System that enables:

- **EV Drivers** to discover nearby charging stations, book charging slots, and monitor charging sessions in real-time
- **Station Owners** to manage their charging stations, view analytics, and monitor equipment status
- **Administrators** to manage the entire platform, users, and stations

### Core Components

| Component | Description | Technology |
|-----------|-------------|------------|
| **Backend API** | RESTful API with real-time WebSocket support | Spring Boot 3.3.5 (Java 21) |
| **Mobile App** | Android application for EV drivers | Kotlin + Jetpack Compose |
| **Web Portal** | Admin and Owner dashboard | React 19 + Redux + Tailwind CSS |
| **Database** | PostgreSQL database hosted on Supabase | PostgreSQL |

---

## 2. Architecture

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           EV CHARGING PLATFORM ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│    ┌─────────────────┐                ┌─────────────────┐                          │
│    │  Android App   │                │   Web Portal    │                          │
│    │  (Kotlin/      │                │   (React +      │                          │
│    │  Compose)       │                │   Redux)         │                          │
│    └────────┬────────┘                └────────┬────────┘                          │
│             │                                  │                                   │
│             │    ┌──────────────────────────────┴──────────────────────┐          │
│             │    │                                                        │          │
│             │    │              LOAD BALANCER / REVERSE PROXY            │          │
│             │    │                    (Nginx - Production)              │          │
│             │    └──────────────────────────────┬──────────────────────┘          │
│             │                                   │                                    │
│             └───────────────────────────────────┼────────────────────────────────┐ │
│                                                 │                                 │ │
│    ┌────────────────────────────────────────────▼────────────────────────────────┐ │
│    │                          BACKEND API (Spring Boot)                            │ │
│    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │ │
│    │  │  REST API   │  │  WebSocket │  │  Scheduled │  │   Stripe    │          │ │
│    │  │ Controllers │  │  (STOMP)   │  │   Tasks    │  │  Integration│          │ │
│    │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │ │
│    │         │                │                │                │                  │ │
│    │         └────────────────┴────────────────┴────────────────┘                  │ │
│    │                                    │                                         │ │
│    │                          ┌─────────▼─────────┐                               │ │
│    │                          │   Service Layer   │                               │ │
│    │                          │ (Business Logic) │                               │ │
│    │                          └─────────┬─────────┘                               │ │
│    │                                    │                                         │ │
│    │         ┌──────────────────────────┼──────────────────────────┐              │ │
│    │         │                          │                          │              │ │
│    │   ┌─────▼─────┐            ┌───────▼───────┐        ┌───────▼───────┐      │ │
│    │   │   JPA     │            │     JWT       │        │    Stripe     │      │ │
│    │   │ Repositor │            │   Security    │        │    Service    │      │ │
│    │   └─────┬─────┘            └────────────────┘        └───────────────┘      │ │
│    └─────────┼───────────────────────────────────────────────────────────────────┘ │
│              │                                                                     │
│              ▼                                                                     │
│    ┌───────────────────────────────────────────────────────────────────────────┐  │
│    │                      DATABASE (PostgreSQL - Supabase)                      │  │
│    │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐   │  │
│    │  │  User  │  │Station │  │ Charger│  │Booking│  │Charging│  │  IoT   │   │  │
│    │  │        │  │        │  │ Slot   │  │        │  │Session│  │ Sensor │   │  │
│    │  └────────┘  └────────┘  └────────┘  └────────┘  └────────┘  └────────┘   │  │
│    └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                     │
│                                              ┌─────────────────┐                     │
│                                              │  ESP32 IoT      │                     │
│                                              │  Sensors        │                     │
│                                              └────────┬────────┘                     │
│                                                       │                             │
│                                                       ▼                             │
│                                              ┌─────────────────┐                     │
│                                              │ Live Power      │                     │
│                                              │ Monitoring      │                     │
│                                              └─────────────────┘                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              USER AUTHENTICATION FLOW                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   1. User opens app/portal                                                       │
│          │                                                                       │
│          ▼                                                                       │
│   2. Splash validates JWT token                                                  │
│          │                                                                       │
│          ├── Token Valid ──────────────────► Home/Dashboard                      │
│          │                                                                       │
│          └── Token Invalid/Expired                                               │
│                    │                                                             │
│                    ▼                                                             │
│          3. Login Screen                                                         │
│                    │                                                             │
│                    ├── Mobile OTP                                                │
│                    │       │                                                     │
│                    │       ▼                                                     │
│                    │   POST /api/auth/send-otp                                    │
│                    │       │                                                     │
│                    │       ▼                                                     │
│                    │   POST /api/auth/validate-otp                                │
│                    │       │                                                     │
│                    │       ├── Valid OTP ──► Return JWT + User                   │
│                    │       │                   │                                  │
│                    │       │                   ▼                                  │
│                    │       │              Save token to                          │
│                    │       │              DataStore/localStorage                 │
│                    │       │                   │                                  │
│                    │       │                   ▼                                  │
│                    │       └──────────────────► Home/Dashboard                   │
│                    │                                                                     │
│                    └── Email/Password                                            │
│                            │                                                      │
│                            ▼                                                      │
│                        POST /api/auth/login                                       │
│                            │                                                      │
│                            ├── Valid ──► Return JWT + User                       │
│                            │                   │                                  │
│                            │                   ▼                                  │
│                            │              Navigate to Home                        │
│                            │                                                                     │
│                            └── Invalid ──► Show Error Message                    │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Technology Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.3.5 | Backend framework |
| Java | 21 | Programming language |
| PostgreSQL | - | Primary database |
| Spring Security | 6.x | Authentication & authorization |
| JWT (jjwt) | 0.12.5 | Token-based security |
| Spring Data JPA | - | ORM framework |
| Spring WebSocket | - | Real-time communication |
| Stripe Java SDK | 25.2.0 | Payment processing |
| Lombok | - | Boilerplate reduction |

### Android App

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9.x | Programming language |
| Jetpack Compose | BOM 2024.02.00 | UI framework |
| Material Design 3 | - | Design system |
| Retrofit | 2.9.0 | HTTP client |
| OkHttp | 4.12.0 | Network layer |
| Navigation Compose | - | Navigation |
| DataStore | - | Local storage |
| Coroutines | - | Async operations |
| Google Maps Compose | - | Maps integration |
| Coil | - | Image loading |

### Web Portal

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 19.0.0 | UI framework |
| Vite | 6.1.0 | Build tool |
| Redux Toolkit | 2.11.2 | State management |
| React Router DOM | 7.12.0 | Routing |
| Tailwind CSS | 4.1.18 | Styling |
| Axios | 1.13.2 | HTTP client |
| Recharts | 2.15.0 | Charts |
| Framer Motion | 12.34.2 | Animations |
| Lucide React | - | Icons |

---

## 4. Features

### Mobile App Features

- [x] **OTP Authentication** - Mobile number based login with OTP verification
- [x] **JWT Token Management** - Secure token storage and auto-refresh
- [x] **Station Discovery** - Interactive map with nearby charging stations
- [x] **Station Details** - View station info, connectors, pricing
- [x] **Slot Booking** - Date/time selection with price estimation
- [x] **Booking Management** - View, start, cancel bookings
- [x] **Live Charging** - Real-time power monitoring during charging
- [x] **Charging History** - View past charging sessions
- [x] **User Profile** - Manage personal information
- [x] **Navigation** - Open station location in Google Maps

### Backend Features

- [x] **JWT Authentication** - Secure token-based login
- [x] **OTP System** - Mobile OTP generation and validation
- [x] **Station Management** - CRUD operations for stations
- [x] **Charger Slots** - Manage charging points per station
- [x] **Booking System** - Create, manage, cancel bookings
- [x] **Overlap Detection** - Prevent double bookings
- [x] **Charging Sessions** - Start/stop charging with cost calculation
- [x] **IoT Integration** - Receive ESP32 sensor data
- [x] **Live Power Data** - Real-time voltage, current, power
- [x] **Stripe Payments** - Payment intent creation and webhook handling
- [x] **WebSocket Notifications** - Real-time slot status updates
- [x] **Scheduled Tasks** - Auto-expire unstarted bookings
- [x] **Station Recommendations** - Algorithm-based station ranking

### Web Portal Features

- [x] **Admin Dashboard** - Platform-wide statistics
- [x] **Owner Dashboard** - Station fleet management
- [x] **Role-Based Access** - Admin and Owner roles
- [x] **Station List** - View and manage stations
- [x] **Responsive Design** - Works on desktop and tablet

---

## 5. Project Structure

```
EV-Project/
│
├── backend/                         # Spring Boot REST API
│   ├── src/main/java/com/ganesh/EV_Project/
│   │   ├── EvProjectApplication.java     # Main application class
│   │   ├── config/                        # Configuration classes
│   │   │   ├── SecurityConfig.java       # Spring Security configuration
│   │   │   ├── JwtUtil.java              # JWT token utilities
│   │   │   ├── JwtRequestFilter.java     # JWT authentication filter
│   │   │   ├── WebSocketConfig.java      # WebSocket configuration
│   │   │   ├── CorsConfig.java           # CORS settings
│   │   │   └── DataSeeder.java           # Initial data seeding
│   │   ├── controller/                   # REST controllers
│   │   │   ├── AuthController.java       # Authentication endpoints
│   │   │   ├── StationController.java    # Station CRUD
│   │   │   ├── StationRecommendationController.java  # Recommendations
│   │   │   ├── ChargerSlotController.java # Slot management
│   │   │   ├── BookingController.java    # Booking operations
│   │   │   ├── ChargingSessionController.java  # Charging sessions
│   │   │   ├── PaymentController.java    # Stripe payments
│   │   │   ├── IoTDataController.java     # IoT sensor data
│   │   │   └── WebSocketController.java   # Real-time updates
│   │   ├── service/                       # Business logic
│   │   │   ├── AuthService.java
│   │   │   ├── StationService.java
│   │   │   ├── StationRecommendationService.java
│   │   │   ├── ChargerSlotService.java
│   │   │   ├── BookingService.java
│   │   │   ├── ChargingSessionService.java
│   │   │   ├── PaymentService.java
│   │   │   ├── OtpService.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── repository/                    # Data access layer
│   │   │   ├── UserRepository.java
│   │   │   ├── StationRepository.java
│   │   │   ├── ChargerSlotRepository.java
│   │   │   ├── BookingRepository.java
│   │   │   ├── ChargingSessionRepository.java
│   │   │   ├── PaymentRepository.java
│   │   │   ├── IoTSensorDataRepository.java
│   │   │   └── OtpRepository.java
│   │   ├── model/                         # JPA entities
│   │   │   ├── User.java
│   │   │   ├── Station.java
│   │   │   ├── ChargerSlot.java
│   │   │   ├── Booking.java
│   │   │   ├── ChargingSession.java
│   │   │   ├── Payment.java
│   │   │   ├── IoTSensorData.java
│   │   │   └── Otp.java
│   │   ├── dto/                           # Data transfer objects
│   │   │   ├── BookingRequest.java
│   │   │   ├── ChargingSessionRequest.java
│   │   │   ├── ViewportResponseDTO.java
│   │   │   ├── StationPinDTO.java
│   │   │   ├── StationMarkerDTO.java
│   │   │   └── StationScoreDTO.java
│   │   ├── enums/                         # Enumerations
│   │   │   ├── Role.java
│   │   │   ├── SlotType.java
│   │   │   ├── SlotStatus.java
│   │   │   ├── ConnectorType.java
│   │   │   ├── BookingStatus.java
│   │   │   └── PaymentStatus.java
│   │   ├── payload/                       # API response wrappers
│   │   │   └── APIResponse.java
│   │   └── exception/                     # Custom exceptions
│   │       ├── ResourceNotFoundException.java
│   │       ├── APIException.java
│   │       └── MyGlobalExceptionHandler.java
│   ├── src/main/resources/
│   │   └── application.properties         # Application configuration
│   └── pom.xml                            # Maven dependencies
│
├── android/                         # Android Kotlin App
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/ganesh/ev/
│   │   │   │   ├── MainActivity.kt         # Main activity & navigation
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/              # Data models
│   │   │   │   │   │   ├── Models.kt       # Core models
│   │   │   │   │   │   ├── StationPin.kt   # Map pin DTOs
│   │   │   │   │   │   ├── StationMarker.kt
│   │   │   │   │   │   ├── StationWithScore.kt
│   │   │   │   │   │   └── LivePowerData.kt
│   │   │   │   │   ├── network/
│   │   │   │   │   │   ├── ApiService.kt   # Retrofit API interface
│   │   │   │   │   │   └── RetrofitClient.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── UserPreferencesRepository.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/            # Compose screens
│   │   │   │   │   │   ├── SplashScreen.kt
│   │   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── StationDetailScreen.kt
│   │   │   │   │   │   ├── SlotBookingScreen.kt
│   │   │   │   │   │   ├── BookingConfirmationScreen.kt
│   │   │   │   │   │   ├── MyBookingsScreen.kt
│   │   │   │   │   │   ├── BookingDetailScreen.kt
│   │   │   │   │   │   ├── ChargingScreen.kt
│   │   │   │   │   │   ├── ChargingHistoryScreen.kt
│   │   │   │   │   │   └── ProfileScreen.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   └── StationCard.kt
│   │   │   │   │   ├── theme/              # Design system
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Type.kt
│   │   │   │   │   │   ├── Shape.kt
│   │   │   │   │   │   ├── ClayComponents.kt
│   │   │   │   │   │   └── ClayModifiers.kt
│   │   │   │   │   └── viewmodel/          # ViewModels
│   │   │   │   │       ├── AuthViewModel.kt
│   │   │   │   │       ├── StationViewModel.kt
│   │   │   │   │       ├── BookingViewModel.kt
│   │   │   │   │       └── ChargingViewModel.kt
│   │   │   │   └── util/
│   │   │   │       └── LocationHelper.kt
│   │   │   ├── res/                        # Resources
│   │   │   │   ├── values/
│   │   │   │   ├── drawable/
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── web/                            # React Web Portal
│   ├── src/
│   │   ├── api/
│   │   │   └── axios.js                  # API client
│   │   ├── components/
│   │   │   ├── auth/
│   │   │   │   ├── PrivateRoute.jsx      # Auth guard
│   │   │   │   └── RoleRoute.jsx         # Role-based guard
│   │   │   ├── common/
│   │   │   │   ├── DataTable.jsx
│   │   │   │   ├── QuickActionCard.jsx
│   │   │   │   ├── StatCard.jsx
│   │   │   │   └── StatusBadge.jsx
│   │   │   └── layout/
│   │   │       ├── DashboardLayout.jsx
│   │   │       ├── Header.jsx
│   │   │       └── Sidebar.jsx
│   │   ├── pages/
│   │   │   ├── admin/
│   │   │   │   ├── DashboardOverview.jsx
│   │   │   │   └── StationsList.jsx
│   │   │   ├── auth/
│   │   │   │   ├── LoginPage.jsx
│   │   │   │   └── RegisterPage.jsx
│   │   │   └── owner/
│   │   │       ├── MyStations.jsx
│   │   │       └── PumpOwnerDashboard.jsx
│   │   ├── store/
│   │   │   ├── index.js
│   │   │   ├── authSlice.js
│   │   │   └── stationSlice.js
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── index.css
│   ├── package.json
│   └── vite.config.js
│
├── README.md
├── IMPLEMENTATION_SUMMARY.md
└── package.json
```

---

## 6. Prerequisites

### Backend Requirements

- Java Development Kit (JDK) 21 or higher
- Apache Maven 3.8+
- PostgreSQL database (Supabase hosted or local)

### Android Requirements

- Android Studio (latest version)
- Android SDK 35
- Kotlin plugin
- Gradle 8.x

### Web Portal Requirements

- Node.js 18+
- npm or yarn

---

## 7. Setup Instructions

### 7.1 Backend Setup

#### Step 1: Clone and Navigate

```bash
cd EV-Project/backend
```

#### Step 2: Configure Database

The application is pre-configured to use Supabase PostgreSQL. To use a local database, update `src/main/resources/application.properties`:

```properties
# Local PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/ev_project
spring.datasource.username=postgres
spring.datasource.password=your_password
```

#### Step 3: Build the Application

```bash
# Clean and build
mvn clean install

# Skip tests (optional)
mvn clean install -DskipTests
```

#### Step 4: Run the Backend

```bash
# Run with Maven
mvn spring-boot:run

# Or run the JAR directly
java -jar target/EV-Project-0.0.1-SNAPSHOT.jar
```

The backend will start at `http://localhost:8080`

---

### 7.2 Android Setup

#### Step 1: Open in Android Studio

1. Open Android Studio
2. File → Open → Navigate to `EV-Project/android`
3. Wait for Gradle sync to complete

#### Step 2: Configure Base URL

The app is configured to connect to the backend at `http://localhost:8080/`. To change this:

Edit `android/app/src/main/java/com/ganesh/ev/data/network/RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:8080/"
```

**Note for Emulator:** Use `http://10.0.2.2:8080/` for Android Emulator

#### Step 3: Build and Run

1. Select a device/emulator
2. Click Run (Shift + F10)

---

### 7.3 Web Portal Setup

#### Step 1: Navigate to Web Directory

```bash
cd EV-Project/web
```

#### Step 2: Install Dependencies

```bash
npm install

# Or with yarn
yarn install
```

#### Step 3: Configure API Base URL

Edit `src/api/axios.js`:

```javascript
const API_BASE_URL = 'http://localhost:8080/api'
```

#### Step 4: Run the Development Server

```bash
npm run dev

# Or with yarn
yarn dev
```

The web portal will start at `http://localhost:5173`

---

## 8. API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/send-otp` | Send OTP to mobile | No |
| POST | `/api/auth/validate-otp` | Validate OTP & login | No |
| POST | `/api/auth/complete-profile` | Complete user profile | No |
| POST | `/api/auth/login` | Email/password login | No |
| POST | `/api/auth/register` | User registration | No |
| GET | `/api/auth/me` | Get current user | Yes |

### Station Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stations` | Get all stations |
| GET | `/api/stations/{id}` | Get station by ID |
| POST | `/api/stations` | Create new station |
| PUT | `/api/stations/{id}` | Update station |
| DELETE | `/api/stations/{id}` | Delete station |
| GET | `/api/stations/viewport` | Stations in map bounds |
| GET | `/api/stations/viewport-nearby` | Viewport + nearby stations |
| GET | `/api/stations/nearby` | Ranked nearby stations |
| GET | `/api/stations/{id}/detail` | Station with scores |

### Charger Slot Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/slots/station/{stationId}` | Get slots by station |
| GET | `/api/slots/station/{stationId}/available` | Get available slots |
| PUT | `/api/slots/{id}/status` | Update slot status |

### Booking Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/bookings/user/{userId}` | Get user bookings |
| POST | `/api/bookings` | Create booking |
| PUT | `/api/bookings/{bookingId}/cancel` | Cancel booking |

### Charging Session Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/charging/start` | Start charging |
| POST | `/api/charging/stop/{sessionId}` | Stop charging |
| GET | `/api/charging/session/{sessionId}` | Get session details |
| GET | `/api/charging/user/{userId}` | Get charging history |

### IoT Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/iot/sensor-data` | Receive sensor data |
| GET | `/api/iot/stations/{stationId}/live-power` | Get live power |

### Payment Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/create-intent/{bookingId}` | Create payment |
| POST | `/api/payments/webhook` | Stripe webhook |
| GET | `/api/payments/booking/{bookingId}` | Get payment status |

### API Response Format

All API responses follow this format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    // Response data
  }
}
```

### Example: Get Stations

```bash
curl -X GET http://localhost:8080/api/stations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Response:

```json
{
  "success": true,
  "message": "Stations fetched successfully",
  "data": [
    {
      "id": 1,
      "name": "Downtown Metro Hub",
      "latitude": 19.0760,
      "longitude": 72.8777,
      "address": "123 Main St, Mumbai",
      "pricePerKwh": 15.0,
      "rating": 4.5
    }
  ]
}
```

---

## 9. Database Schema

### Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│      User       │       │    Station      │       │   ChargerSlot   │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │       │ id (PK)         │
│ mobileNumber    │◄──────│ owner_id (FK)    │───────│ station_id (FK) │
│ email           │       │ name            │       │ slotLabel       │
│ name            │       │ latitude        │       │ slotType        │
│ password        │       │ longitude       │       │ connectorType   │
│ role            │       │ address         │       │ powerKw         │
│ isFirstTimeUser │      │ pricePerKwh    │       │ status          │
└────────┬────────┘       │ rating          │       └────────┬────────┘
         │                └────────┬────────┘                │
         │                         │                         │
         │                         │                         │
         │                ┌────────▼────────┐       ┌────────▼────────┐
         │                │     Booking      │       │  IoTSensorData   │
         │                ├─────────────────┤       ├─────────────────┤
         │                │ id (PK)         │       │ id (PK)         │
         └───────────────►│ user_id (FK)   │       │ station_id (FK) │
                          │ slot_id (FK)    │◄──────│ voltage         │
                          │ startTime       │       │ current         │
                          │ endTime         │       │ power           │
                          │ status          │       │ timestamp       │
                          │ priceEstimate   │       └─────────────────┘
                          └────────┬────────┘
                                   │
                                   │
                          ┌────────▼────────┐       ┌─────────────────┐
                          │ChargingSession  │       │    Payment      │
                          ├─────────────────┤       ├─────────────────┤
                          │ id (PK)         │       │ id (PK)         │
                          │ booking_id (FK) │◄──────│ booking_id (FK)│
                          │ startTime       │       │ amount          │
                          │ endTime         │       │ stripePaymentId │
                          │ energyKwh       │       │ status          │
                          │ totalCost       │       │ currency        │
                          └─────────────────┘       └─────────────────┘
```

### User Roles

| Role | Description | Access Level |
|------|-------------|--------------|
| CUSTOMER | EV driver | Mobile app only |
| STATION_OWNER | Station operator | Web portal (Owner) |
| ADMIN | Platform admin | Web portal (Admin) |

### Booking Status

| Status | Description |
|--------|-------------|
| PENDING | Booking created, awaiting confirmation |
| CONFIRMED | Booking confirmed, slot reserved |
| ONGOING | Charging in progress |
| COMPLETED | Charging finished |
| EXPIRED | Not started within 15 minutes |
| CANCELLED | User cancelled |

### Slot Status

| Status | Description |
|--------|-------------|
| AVAILABLE | Ready for booking |
| RESERVED | Temporarily reserved |
| BOOKED | Booked by user |
| CHARGING | Currently in use |
| MAINTENANCE | Under maintenance |
| OCCUPIED | Occupied (non-charging) |

---

## 10. User Flows

### Mobile App User Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MOBILE APP USER FLOW                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  START                                                                      │
│    │                                                                        │
│    ▼                                                                        │
│  ┌─────────────────────────────────────────┐                                │
│  │         Splash Screen                    │                                │
│  │  - Check JWT token validity             │                                │
│  │  - Check if first-time user             │                                │
│  └────────────┬────────────────────────────┘                                │
│               │                                                           │
│       ┌───────┴───────┐                                                    │
│       │               │                                                    │
│   [Token Valid]   [Token Invalid/                                            │
│                   First Time]                                                │
│       │               │                                                    │
│       ▼               ▼                                                    │
│  ┌────────────┐  ┌─────────────────────────┐                                │
│  │   Home     │  │    Onboarding           │                                │
│  │   Screen   │  │    (4 slides)           │                                │
│  └─────┬──────┘  └───────────┬─────────────┘                                │
│        │                      │                                              │
│        │                      ▼                                              │
│        │               ┌──────────────┐                                     │
│        │               │ Login Screen │                                     │
│        │               │ - Mobile #  │                                     │
│        │               └──────┬───────┘                                     │
│        │                      │                                              │
│        │                      ▼                                              │
│        │               ┌──────────────┐                                     │
│        │               │ OTP Screen   │                                     │
│        │               │ - Enter OTP  │                                     │
│        │               └──────┬───────┘                                     │
│        │                      │                                              │
│        │         ┌────────────┴────────────┐                                 │
│        │         │                         │                                 │
│        │    [New User]              [Existing User]                         │
│        │         │                         │                                 │
│        │         ▼                         │                                 │
│        │  ┌────────────────┐               │                                 │
│        │  │ Profile Screen │               │                                 │
│        │  │ - Name/Email   │               │                                 │
│        │  └───────┬────────┘               │                                 │
│        │          │                       │                                 │
│        └──────────┼───────────────────────┘                                 │
│                   │                                                         │
│                   ▼                                                         │
│            ┌──────────────┐                                                 │
│            │  Home Screen │                                                 │
│            │  - Map View  │                                                 │
│            │  - Stations  │                                                 │
│            └──────┬───────┘                                                 │
│                   │                                                         │
│                   ▼                                                         │
│            ┌──────────────┐                                                 │
│            │Station Detail│                                                 │
│            │ - Slots      │                                                 │
│            │ - Connectors │                                                 │
│            └──────┬───────┘                                                 │
│                   │                                                         │
│                   ▼                                                         │
│            ┌──────────────┐                                                 │
│            │ Slot Booking  │                                                 │
│            │ - Date/Time   │                                                 │
│            │ - Price Est.  │                                                 │
│            └──────┬───────┘                                                 │
│                   │                                                         │
│                   ▼                                                         │
│            ┌──────────────────┐                                              │
│            │Booking Confirm   │                                              │
│            │ - Booking ID    │                                              │
│            └──────┬───────────┘                                              │
│                   │                                                         │
│                   ▼                                                         │
│            ┌──────────────┐                                                 │
│            │ My Bookings  │                                                 │
│            │ - List       │                                                 │
│            └──────┬───────┘                                                 │
│                   │                                                         │
│              ┌────┴────┐                                                    │
│              │         │                                                    │
│         [Start]    [Cancel]                                                 │
│              │         │                                                    │
│              ▼         │                                                    │
│     ┌────────────┐    │                                                    │
│     │Charging    │    │                                                    │
│     │ - Live Pwr │    │                                                    │
│     │ - Cost     │    │                                                    │
│     └─────┬──────┘    │                                                    │
│           │           │                                                    │
│           ▼           │ ┌────────                                                    │
│    ───┐     │                                                    │
│     │Charging   │     │                                                    │
│     │Complete   │     │                                                    │
│     └───────────┘     │                                                    │
│                       │                                                    │
│                       ▼                                                    │
│                  [Deleted]                                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Configuration

### Backend Configuration (application.properties)

```properties
# Server Configuration
server.port=8080
server.address=0.0.0.0

# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/ev_project
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT Configuration
jwt.secret=your-secret-key-here-change-in-production
jwt.expiration=86400000

# Stripe Configuration
stripe.api.key=sk_test_your_stripe_key
stripe.webhook.secret=whsec_your_webhook_secret

# Logging
logging.level.org.springframework.security=DEBUG
```

### Android Configuration

In `RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "http://localhost:8080/"
```

### Web Portal Configuration

In `axios.js`:

```javascript
const API_BASE_URL = 'http://localhost:8080/api'
```

---

## 12. Running the Application

### Running Backend

```bash
cd backend

# Development mode
mvn spring-boot:run

# Production mode
java -jar target/EV-Project-0.0.1-SNAPSHOT.jar
```

### Running Android App

1. Open Android Studio
2. Select the `android` folder
3. Run on device/emulator

### Running Web Portal

```bash
cd web
npm run dev
```

Access at: `http://localhost:5173`

---

## 13. Testing

### Test Authentication Flow

```bash
# 1. Send OTP
curl -X POST "http://localhost:8080/api/auth/send-otp?mobileNumber=9876543210"

# 2. Validate OTP (use OTP from response or backend logs)
curl -X POST "http://localhost:8080/api/auth/validate-otp?mobileNumber=9876543210&otp=123456"

# 3. Get Stations with JWT token
curl -X GET "http://localhost:8080/api/stations" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Test Booking Flow

```bash
# 1. Get available slots
curl -X GET "http://localhost:8080/api/slots/station/1/available" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. Create booking
curl -X POST "http://localhost:8080/api/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "userId": 1,
    "slotId": 1,
    "startTime": "2024-12-01T10:00:00",
    "endTime": "2024-12-01T12:00:00"
  }'
```

### Test IoT Integration

```bash
# Simulate ESP32 sensor data
curl -X POST "http://localhost:8080/api/iot/sensor-data" \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": 1,
    "voltage": 220.0,
    "current": 15.0
  }'
```

---

## 14. Deployment

### Backend Deployment (AWS Elastic Beanstalk)

1. **Create JAR**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Upload to Elastic Beanstalk**
   - Go to AWS Elastic Beanstalk console
   - Create new application
   - Upload the JAR file
   - Configure environment (Java 21)

3. **Environment Variables**
   - Set `SPRING_PROFILES_ACTIVE=production`
   - Configure database credentials
   - Set JWT secret key

### Database (Supabase)

1. Create a Supabase project
2. Get connection string from settings
3. Update `application.properties`

### Android App Build

```bash
cd android
./gradlew assembleRelease
```

APK will be generated at `android/app/build/outputs/apk/release/`

### Web Portal Build

```bash
cd web
npm run build
```

Static files will be in `web/dist/`

---

## 15. Screenshots

### Mobile App Screens

| Screen | Description |
|--------|-------------|
| Splash | App logo with loading indicator |
| Onboarding | 4-slide feature introduction |
| Login | Mobile number input with OTP |
| Home | Google Maps with station markers |
| Station Detail | Station info with charger slots |
| Slot Booking | Date/time picker with price |
| Charging | Live power and cost display |

### Web Portal Screens

| Page | Description |
|------|-------------|
| Login | Email/password login |
| Admin Dashboard | Platform statistics and charts |
| Owner Dashboard | Station fleet overview |
| Stations List | Grid view of all stations |

---

## 16. Future Improvements

### Backend Enhancements

- [ ] Redis caching for improved performance
- [ ] Rate limiting for API endpoints
- [ ] Email/SMS OTP delivery (Twilio/MSG91)
- [ ] Push notifications (FCM)
- [ ] Docker containerization
- [ ] CI/CD pipeline
- [ ] Unit and integration tests
- [ ] API documentation (Swagger/OpenAPI)

### Android Enhancements

- [ ] Push notifications
- [ ] Offline mode
- [ ] Dark mode
- [ ] Widget for quick access
- [ ] AR navigation to stations
- [ ] Charging cost calculator
- [ ] Favorite stations

### Web Portal Enhancements

- [ ] Complete Owner dashboard
- [ ] User management
- [ ] Booking management
- [ ] Advanced analytics
- [ ] Real-time notifications
- [ ] Settings pages

---

## 17. License

This project is for educational and demonstration purposes.

---

## Support

For issues or questions:

1. Check the `IMPLEMENTATION_SUMMARY.md` for detailed implementation notes
2. Review the backend logs for error details
3. Verify database connectivity
4. Check API endpoints with Postman/cURL

---

**Project Status: ✅ Complete and Functional**

Built with ❤️ for the EV community
