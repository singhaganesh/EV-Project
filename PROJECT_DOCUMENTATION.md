# EV Charging Management System - Documentation

## Overview

A full-stack EV charging station management platform with three modules: backend API, admin web dashboard, and Android mobile app.

---

## Architecture

```
EV-Project/
├── backend/          # Spring Boot REST API
├── web/              # React Admin Dashboard
└── android/          # Android Mobile App
```

---

## Backend (`backend/`)

### Tech Stack
- **Runtime:** Java 21
- **Framework:** Spring Boot 3.3.5
- **Database:** PostgreSQL with JPA/Hibernate
- **Security:** Spring Security + JWT Authentication
- **Real-time:** WebSocket (STOMP)
- **Payments:** Stripe API

### Key Dependencies
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database ORM
- `spring-boot-starter-security` - Authentication
- `spring-boot-starter-websocket` - Real-time updates
- `jjwt-0.12.5` - JWT tokens
- `stripe-java-25.2.0` - Payment processing
- `postgresql` - Database driver

### Structure
```
src/main/java/com/ganesh/EV_Project/
├── controller/      # REST endpoints (Auth, Booking, Station, Payment, etc.)
├── service/         # Business logic
├── repository/      # Data access layer
├── model/           # JPA entities
├── dto/             # Data transfer objects
├── config/          # Security, JWT, CORS, WebSocket configs
├── exception/       # Custom exceptions & handlers
├── enums/           # Enumerations
└── payload/         # API response wrappers
```

### Run
```bash
cd backend
./mvnw spring-boot:run
# or
java -jar target/EV-Project-0.0.1-SNAPSHOT.jar
```

---

## Web Dashboard (`web/`)

### Tech Stack
- **Framework:** React 19
- **Build Tool:** Vite 6
- **Styling:** TailwindCSS 4
- **State Management:** Redux Toolkit
- **UI Library:** Material UI (MUI) v7
- **Routing:** React Router v7
- **HTTP Client:** Axios
- **Charts:** Recharts
- **Maps:** Leaflet + React-Leaflet
- **Animations:** Framer Motion

### Key Features
- Admin dashboard with statistics
- Station owner management portal
- Real-time station availability
- Booking management
- Responsive design with dark/light mode support

### Structure
```
src/
├── api/           # Axios configuration
├── components/    # Reusable UI components
│   ├── admin/     # Admin-specific components
│   ├── auth/      # Authentication components
│   ├── common/    # Shared components
│   ├── layout/    # Layout components (Header, Sidebar)
│   └── owner/     # Station owner components
├── pages/         # Route pages
│   ├── admin/     # Admin pages
│   ├── auth/      # Login, Register
│   └── owner/     # Owner dashboard pages
├── store/         # Redux slices
├── App.jsx        # Main app with routing
└── main.jsx       # Entry point
```

### Run
```bash
cd web
npm install
npm run dev
```

### Build
```bash
npm run build
```

---

## Android App (`android/`)

### Tech Stack
- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose (BOM 2024.04.01)
- **Architecture:** MVVM with ViewModels
- **Networking:** Retrofit + OkHttp
- **Real-time:** STOMP WebSocket
- **DI:** Manual dependency injection

### Key Dependencies
- `androidx.compose.material3` - Material 3 UI
- `retrofit2` - REST API client
- `okhttp3` - HTTP client
- `androidx.navigation.compose` - Navigation
- `androidx.lifecycle` - ViewModel & LiveData

### Structure
```
app/src/main/java/com/ganesh/ev/
├── data/
│   ├── model/        # Data classes
│   ├── network/       # Retrofit, API service, WebSocket client
│   └── repository/    # Data repositories
├── ui/
│   ├── screens/       # Compose screens
│   ├── viewmodel/     # ViewModels
│   └── theme/         # Colors, Typography, Shapes, Components
├── util/              # Utilities
└── MainActivity.kt    # Entry point
```

### Key Screens
- Splash & Onboarding
- Login/Register
- Home (map with stations)
- Station Detail
- Slot Booking
- Charging (real-time session)
- Booking History
- Profile

### Build
```bash
cd android
./gradlew assembleDebug
```

### APK Location
```
android/app/build/outputs/apk/debug/app-debug.apk
```

---

## API Endpoints (Backend)

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/otp/send` - Send OTP
- `POST /api/auth/otp/verify` - Verify OTP

### Stations
- `GET /api/stations` - Get all stations
- `GET /api/stations/viewport` - Get stations in map viewport
- `POST /api/stations` - Create station (owner)
- `GET /api/stations/{id}` - Get station details
- `PUT /api/stations/{id}` - Update station
- `GET /api/stations/recommendations` - Get recommended stations

### Bookings
- `POST /api/bookings` - Create booking
- `GET /api/bookings/{id}` - Get booking details
- `PUT /api/bookings/{id}/cancel` - Cancel booking
- `GET /api/bookings/user/{userId}` - User's bookings

### Charging Sessions
- `POST /api/charging/start` - Start charging
- `POST /api/charging/stop` - Stop charging
- `GET /api/charging/session/{id}` - Get session status

### Payments
- `POST /api/payments/create-intent` - Create Stripe payment intent
- `POST /api/payments/confirm` - Confirm payment

### WebSocket
- `/ws` - STOMP endpoint for real-time updates

---

## Configuration

### Backend (.env)
```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ev_charging
DB_USER=postgres
DB_PASSWORD=password

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# Stripe
STRIPE_SECRET_KEY=sk_test_xxx
STRIPE_PUBLISHABLE_KEY=pk_test_xxx

# Server
SERVER_PORT=8080
```

### Web (.env)
```properties
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## User Roles

| Role | Description |
|------|-------------|
| `USER` | EV owner - browse stations, book slots, charge |
| `OWNER` | Station owner - manage stations and slots |
| `ADMIN` | Platform admin - manage all stations and users |

---

## Data Models

### User
- id, name, email, phone, password, role, createdAt

### Station
- id, name, owner, address, latitude, longitude, status, pricing

### ChargerSlot
- id, station, slotType, connectorType, status, pricePerKwh

### Booking
- id, user, slot, vehicleType, startTime, endTime, status, totalCost

### ChargingSession
- id, booking, startTime, endTime, energyDelivered, status

### Payment
- id, booking, amount, stripePaymentId, status, createdAt

---

## Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- PostgreSQL 14+
- Android SDK (for mobile app)

### Setup

1. **Clone & setup database:**
   ```sql
   CREATE DATABASE ev_charging;
   ```

2. **Start backend:**
   ```bash
   cd backend
   cp .env.example .env  # Configure environment
   ./mvnw spring-boot:run
   ```

3. **Start web dashboard:**
   ```bash
   cd web
   npm install
   npm run dev
   ```

4. **Build Android APK:**
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

---

## Version

- **Backend:** 0.0.1-SNAPSHOT
- **Web:** 0.0.0
- **Android:** Debug build

---

## License

MIT License
