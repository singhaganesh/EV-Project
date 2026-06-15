# 📦📦 PROJECT OVERVIEW REPORT

## 🎯🎯 What Is This Project
This is a smart EV Charging Management System (conceptualized as an "Uber for EV charging") that enables electric vehicle drivers to locate, reserve, and pay for charging sessions in real time. It consists of a native Android mobile application for drivers, a React-based web dashboard for station owners and administrators, and a centralized Spring Boot backend engine. The platform provides automated slot allocation, a live physics-based telemetry simulation, and secure payment processing.

## 👥👥 User Types
1. **EV Drivers (Customers / End Users)**:
   - Access the platform via the native Android app.
   - Can discover nearby stations, check slot availability, book specific connector types, start/stop charging, track charging telemetry in real time, and pay for sessions.
2. **Station Owners (Pump Owners / Partners)**:
   - Access the platform via the React web dashboard under `/owner`.
   - Can manage their station fleet, configure dispensaries, modify gun/connector settings (e.g., setting guns to maintenance), view live telemetry metrics of chargers, track daily/weekly earnings, and manage payouts.
3. **Administrators (System Admins)**:
   - Access the platform via the React web dashboard under `/admin`.
   - Monitor the entire charging network, track active stations, and oversee general system bookings, user accounts, and billing logs.

## 🔥🔥 Core Problem Being Solved
Finding available, working, and compatible EV chargers is a major source of anxiety for EV owners (daily commuters and commercial fleet operators). This project solves these pain points by:
- Providing real-time, high-accuracy slot availability instead of static map information.
- Eliminating double-booking conflicts through instant slot reservations.
- Offering specialized pricing and capacity settings for heavy-duty commercial vehicles (electric trucks) versus passenger cars.
- Automating hardware utilization and payment payouts for station owners.

## 🔄🔄 Primary User Flow
1. **Station Discovery**: The EV Driver opens the Android app, which queries the backend to display nearby charging stations on an interactive map using geographic viewport coordinates.
2. **Slot Reservation**: The driver selects a station, chooses a compatible connector (e.g., CCS2, Type 2) and vehicle type (Car or Truck), and registers a booking. The slot status changes to `BOOKED` for a 20-minute grace period.
3. **Initiating Charge**: Upon arrival, the driver connects their vehicle and taps "Start Charging." The backend transitions the slot status to `CHARGING` and activates a background physics simulation.
4. **Live Telemetry Monitoring**: As the vehicle charges, the app and the station owner's dashboard display real-time variables (State of Charge, power in kW, voltage, current, connector temperature, and accumulative cost) via WebSocket/STOMP channels.
5. **Stopping and Billing**: The driver stops the session. The simulator halts, calculates the final cost, creates a Razorpay transaction order, and prompts payment.
6. **Payment & Release**: The driver completes payment securely. Once verified, the slot status resets to `AVAILABLE`, and the station's last used time and earnings metrics are updated.

## 🏗🏗️ Architecture Diagram
```text
           +-------------------------------+       +--------------------------------+
           | Android App (Kotlin/Compose)  |       | React Web (Vite/Redux/MUI)     |
           | - Map discovery & reservation |       | - Owner fleet & billing        |
           | - Live telemetry progress bar |       | - Admin console monitoring     |
           +---------------+---------------+       +---------------+----------------+
                           |                                       |
                   HTTP / REST (JWT)                       HTTP / REST (JWT)
                           |               +---------------+-------+
                           |               | Websocket (STOMP status updates)
                           |               |
                   +-------v---------------+v-------+
                   | Spring Boot Java 21 Monolith   |
                   | - Security & Auth Filter Chain |
                   | - Smart Slot Allocator Service |
                   | - Live Physics Simulator Engine|
                   +---------------+----------------+
                                   |
                           JPA / Hibernate
                                   |
                   +---------------v----------------+
                   | PostgreSQL Database (Supabase) |
                   | - Coordinate indices (Lat/Lng) |
                   | - Pessimistic write locks      |
                   +--------------------------------+
```

## 📚📚 Tech Stack (Complete)

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Languages** | Java | 21 | Backend core language |
| | Kotlin | 1.9.x | Android app development |
| | JavaScript / JSX | ES6+ | Web dashboard pages and components |
| **Frontend (Web)** | React | 19.0.0 | User interface library |
| | Vite | 6.1.0 | Fast bundler and development server |
| | Redux Toolkit | 2.11.2 | Global state management |
| | React Router DOM | 7.12.0 | Client-side routing and page guards |
| | Material UI (MUI) | 7.3.7 | Core pre-built UI components |
| | Tailwind CSS | 4.1.18 | Utility-first CSS styling |
| | Leaflet / React Leaflet | 1.9.4 / 5.0.0 | Interactive map rendering |
| | Recharts | 2.15.0 | Visualization of earnings and analytics |
| | Framer Motion | 12.34.2 | Fluid animations and transitions |
| | Axios | 1.13.2 | REST API calls with interceptors |
| **Frontend (Mobile)** | Android SDK | SDK 34 target | Native mobile OS framework |
| | Jetpack Compose | Compose BOM | Declarative UI toolkit |
| | Retrofit 2 | 2.9.x | Type-safe REST client |
| | OkHttp WebSocket | 4.12.x | Low-level WebSocket client foundation |
| | Jetpack DataStore | 1.1.x | Local encrypted user preference storage |
| **Backend** | Spring Boot | 3.3.5 | Monolithic backend framework |
| | Spring Security | 3.3.5 | Security authorization and filter chains |
| | Spring Data JPA | 3.3.5 | Object-Relational Mapping (Hibernate) |
| | Spring WebSocket | 3.3.5 | STOMP message broker infrastructure |
| | JJWT | 0.12.5 | JSON Web Token generation and validation |
| **Databases & Storage** | PostgreSQL | 15.x | Relational database storage |
| | Supabase | Managed | Remote host provider for PostgreSQL |
| **DevOps & Infra** | Docker / Docker Compose | v24+ / v2.x | Multi-container encapsulation and orchestration |
| | Maven | 3.9.x | Backend dependency builder |
| **Testing** | JUnit 5 | 5.x | Backend unit testing |
| | Mockito | 5.x | Mocking utility for unit assertions |
| **External Integrations**| Stripe Java SDK | 25.10.0 | Credit card intent payments and webhooks |
| | Razorpay SDK | 1.4.7 | Gateway transactions and payment verification |
| | Nominatim API | (OSM) | Address reverse geocoding lookup |
| | Google Maps SDK | - | Android map rendering and user location helper |

## 🗂🗂️ Major Modules

| Module | Description | Status |
| :--- | :--- | :--- |
| **Authentication & Profile** | Stateless JWT parsing, OTP request / validation, and complete-profile workflow. | **Complete** |
| **Map Discovery & Geolocation** | Bounding box queries for map pins with in-memory viewport caching to throttle redundant network requests. | **Complete** |
| **Slot Reserver (Smart Assigner)** | Handles vehicle-to-slot matching logic with fallback checks and database-level pessimistic write locking. | **Complete** |
| **Physics Simulation Engine** | A scheduled service generating simulated telemetry (voltage, current, Joule heat, and charge tapering). | **Complete** |
| **Payment Verification Gateways** | Integrates with Stripe (intents/webhooks) and Razorpay (order creation/signature validation). | **Complete** |
| **Owner Dashboard Analytics** | Chart displays for earnings, dispatcher configuration, and gun maintenance status controls. | **Complete** |
| **Admin Panel Control** | Platform-wide station management. | **In Progress** (Placeholder routes for users/bookings) |

## 📊📊 Project Maturity Assessment
- **Overall stage**: **Beta / Near Production**
- **Estimated completion**: **85%**
- **What's built**:
  - Full backend persistence and schema definitions with Spring Boot REST/WS routing.
  - Live STOMP WebSocket telemetry ticker simulation.
  - Complete customer mobile app flow from onboarding, mapping, checkouts, live charging views, to payment.
  - Interactive partner web dashboard for station stats, trend lines, and dispenser/gun maintenance configurations.
- **What's missing**:
  - Implementation of Admin-specific user logs, bookings auditing, and system settings pages (currently returns "Coming Soon" page placeholders).
  - Production SMS/email gateways for OTP delivery (presently simulated).
- **Biggest gaps**:
  - Hardcoded test credentials (e.g., bypass rules using `password123`) in `AuthController.java` must be eliminated.
  - Missing admin dashboard pages need implementation before platform scale-up.

## 🧭🧭 Technical Vision Summary
The project was designed as a **highly responsive, tactile, and network-efficient platform**. The architecture follows:
1. **Tactile Aesthetics (Claymorphism)**: Incorporating complex Android canvas shaders to create a soft, premium 3D physical-card illusion on mobile.
2. **Network Optimizations**: Employing a divided viewport approach—returning coordinates-only pins for broad queries and reserving full data objects only for the top 5 nearest stations, avoiding N+1 REST bottlenecks.
3. **Hardware Independence**: Implementing a detailed physics simulation engine to model battery characteristics (like SoC charging tapers) to facilitate testing without requiring physical charger hardware.
4. **Data Integrity (Concurrency Control)**: Relying on row-level database locks (`SELECT ... FOR UPDATE`) in repositories to secure slots under heavy concurrent demand.
