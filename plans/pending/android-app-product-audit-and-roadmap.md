# Android Driver App — Product Audit & Production Roadmap ("Plugsy")

**Author lens:** Mobile Product Manager + Principal Android Architect
**Date:** 2026-06-16
**Scope:** The **consumer/driver Android app** only (`android/`, package `com.ganesh.ev`). The React owner/admin dashboard and Spring Boot backend are referenced only where the mobile experience depends on them.
**App state:** `versionCode 1` / `versionName 1.0`, `minSdk 24`, `targetSdk 35`, release base URL `https://api.plugsy.in/`. Pre-first-release.
**Benchmarks:** ChargePoint, EVgo, Electrify America, PlugShare (global); Statiq, Kazam, Tata Power EZ Charge, ChargeZone (India, the actual competitive set).

> **Relationship to the existing audit.** A full-stack `plans/pending/missing-features-audit.md` already exists (system-wide, 2026-06-12). This document does **not** replace it — it goes deeper on the *mobile client* with concrete Compose/Retrofit/StateFlow integration notes, and isolates Android-only gaps (background charging service, offline cache, on-device token persistence, deep links) that a system-wide audit doesn't separate out. Where priorities overlap (FCM, account deletion, reviews, route planning), they are kept consistent with that document.

---

## Section 1: Detailed List of Current Features (Audited)

Each item below was **verified against source**, not just the feature brief. ✅ = present and wired; ⚠️ = present but with a caveat that affects production.

### 1.1 Authentication & Session
- ✅ **Mobile-number OTP** — `send-otp` / `validate-otp` (`ApiService.kt`). Profile completion captures **Name, Email, Role** (`CompleteProfileRequest`).
- ✅ **Token lifecycle** — access + refresh tokens persisted in DataStore (`UserPreferencesRepository`); auto-login via `SplashScreen` (`onAuthValid` / `onAuthExpired`).
- ✅ **Silent 401 recovery** — `RetrofitClient` registers an OkHttp `Authenticator` that calls `refresh-token` on a separate client (no interceptor recursion) and retries, capped at 3 attempts.
- ⚠️ **Token-rotation persistence bug** — on refresh, the `Authenticator` updates the **in-memory** `authToken` only. It never writes the new token back to DataStore, and never updates the rotating refresh token. After a process restart the app reloads the *stale* token from DataStore and depends on another 401 round-trip to recover (and if the backend rotates refresh tokens, the user is silently logged out). *Real bug, not a feature gap — see roadmap M-blocker.*

### 1.2 Onboarding
- ✅ **4-page slider** (`OnboardingScreen`), gated by a DataStore `onboardingCompleted` flag, shown once before login.

### 1.3 Map / Discovery (`HomeScreen`)
- ✅ Google Maps Compose with custom pins; **viewport-debounced** fetching via `getViewportWithNearby` (full data for top-N, lightweight pins for the rest — a genuinely good bandwidth optimization).
- ✅ List/map toggle, **distance sort**, relative "last used" time, bottom station pager.
- ⚠️ **No discovery-level filtering or text search** — connector type / charging speed / "available now" / network filters exist only on the *detail* screen (see 1.4), not on the map/list. No search-by-name/address box.

### 1.4 Station Details (`StationDetailScreen`)
- ✅ Operating-hours parsing with live open/closed computation; guns grouped by dispensary unit.
- ✅ Filter chips (**Available / AC / DC**); **static** map preview.
- ✅ Live grid metrics (Voltage, Current, Power, Forecasted Load) via `getStationLivePower` (`api/iot/...`).
- ✅ Weighted scores (Traffic, Grid, Parking, Accessibility) via `getStationDetail`.
- ⚠️ **`Station.rating` is display-only** — there is no write path (no reviews); the recommendation scoring reads a value nothing populates.

### 1.5 Slot Booking (`SlotBookingScreen` + shared `BookingViewModel`)
- ✅ Instant **20-minute reservation**, connector selection (CCS2 / Type-2), vehicle class (Car / Truck), and the **truck-fallback** prompt when car slots are exhausted.
- ⚠️ **Instant-only** — no reserve-ahead / scheduled / recurring booking.

### 1.6 Booking Management
- ✅ Active-reservation **countdown timer**, booking detail with **cancel**, paginated history (`getUserBookings`, page/size).

### 1.7 Real-Time Charging (`ChargingScreen` + `ChargingViewModel` + `StompClient`)
- ✅ STOMP-over-WebSocket telemetry: live **SoC%** (pulsing ring), power, elapsed cost, energy, remaining time, **emergency stop**.
- ✅ **Resilient socket** — `StompClient` reconnects with exponential backoff (2s→30s cap) and re-subscribes all topics on reconnect; JWT sent in the CONNECT frame.
- ⚠️ **No background survival** — the session lives in a `ChargingViewModel` **constructed in `MainActivity.onCreate`** and an in-process OkHttp socket. There is **no foreground service** and **no `FOREGROUND_SERVICE` permission**. When the screen sleeps / app is backgrounded / Doze kicks in, the OS tears down the socket and (on process death) the session state is lost — recoverable only by re-fetching `getSessionByBooking`. For an app whose core promise is "watch your charge," this is the single biggest experiential risk.
- ⚠️ STOMP heartbeat negotiated as `0,0` — half-open connections rely on TCP/OkHttp timeouts to be noticed.

### 1.8 Payment & Checkout
- ✅ Invoice summary, **Razorpay** checkout (card/UPI), server-side **signature verification** (`payments/verify`), success screen. `MainActivity` implements `PaymentResultWithDataListener` and routes the result into the shared `ChargingViewModel`.
- ⚠️ **No stored payment instrument, wallet, or receipt artifact** — Razorpay is re-entered every time; `PaymentSuccessScreen` produces no downloadable/emailed receipt or GST invoice.

### 1.9 Profile (`ProfileScreen`)
- ⚠️ **Read-only**. Shows name, email, role, mobile, member-since, last-updated, and **Logout** — nothing else. No edit, no vehicles, no payment methods, no settings, no notification prefs, no help, **no account deletion**.

### 1.10 Architecture baseline (enabling context for Section 3)
- ✅ MVVM + `StateFlow`; Navigation-Compose with a single sealed `Screen` graph in `MainActivity.kt`; Material 3 + custom "Clay" theme; Coil for images.
- ⚠️ **No dependency injection** (Hilt/Koin absent) — ViewModels and repositories are constructed by hand (`ChargingViewModel()` in the Activity; `UserPreferencesRepository(context)` passed down). `MainActivity.kt` is ~550 lines and owns navigation, auth state, and payment callbacks.
- ⚠️ **Persistence is DataStore-only** — **no Room, no WorkManager** → zero offline cache and no durable background work/retry.
- ⚠️ **No FCM/Firebase** anywhere (no `google-services` plugin, no messaging dependency) despite `POST_NOTIFICATIONS` being declared.
- ⚠️ **No deep links / App Links** — manifest has only the `LAUNCHER` intent-filter.
- ⚠️ **No crash reporting / analytics** SDK (Crashlytics, Sentry, or equivalent).

---

## Section 2: Categorized Gaps & Missing Features

Benchmarked against the competitive set. Items are grouped by category; severity is rationalized in Section 3.

### A. Reliability, Background & Offline  *(the mobile-only differentiators an app-level audit must own)*
- **A1 — Background charging foreground service.** A started/bound `Service` (type `dataSync`/`connectedDevice`) hosting `StompClient`, with an **ongoing notification** showing live SoC and a stop action. The current design loses telemetry the moment the user leaves the screen.
- **A2 — Offline cache (Room).** Last-known stations, active booking, and history readable with no network. Today a dead zone *at the charger* (the exact moment of need) yields a blank app.
- **A3 — Persist rotated tokens.** Fix the `Authenticator` to write refreshed access/refresh tokens back through `UserPreferencesRepository`.
- **A4 — Graceful global sign-out.** When refresh ultimately fails, broadcast a single "session expired" event that routes to `login` and clears state (today refresh-failure just returns `null` with no app-level reaction).

### B. Notifications & Alerts
- **B1 — Push (FCM).** Device-token registration + handling for: **charging complete**, **reservation expiring** (T-5 / T-1 min on the 20-min hold), **session force-stopped & billed**, payment outcome. Backend already has the trigger points (force-stop, expiry scheduler, completion).
- **B2 — In-app notification preferences** (per-category toggles) + the Android 13+ runtime `POST_NOTIFICATIONS` request flow (declared, not yet requested).

### C. Profile & Vehicle Management
- **C1 — Vehicle garage.** Make/model/**battery kWh**/preferred connector. Powers accurate "remaining time," auto-selects the connector at booking, and is the prerequisite for route planning (G1).
- **C2 — Editable profile** (name/email/photo) — currently impossible in-app.
- **C3 — Settings hub** (notifications, theme, language, units), Help/FAQ, contact support, app version.

### D. Payment, Wallet & Loyalty
- **D1 — Saved payment methods / wallet.** Tokenized card or **prepaid wallet with auto-reload** — the dominant pattern for Statiq/Kazam in India; removes checkout friction every session.
- **D2 — Receipts & GST invoices** surfaced in-app (download/share/email), essential for the stated **truck/fleet** segment claiming input credit.
- **D3 — Promo codes / referrals / loyalty.** Promo field at checkout, referral share, points or session-streak rewards.
- **D4 — Auto-charge on session end** (post-paid), removing the manual checkout step entirely for wallet/tokenized users.

### E. Navigation & Route Planning
- **E1 — Turn-by-turn hand-off.** A "Directions" action firing a `google.navigation:q=` intent (and an in-app static→interactive map). Cheap, high-utility, currently absent.
- **E2 — Corridor / trip planning.** Origin→destination with charging stops along the route (PlugShare/ABRP's signature feature; especially relevant for inter-city trucking).

### F. Discovery & Social Trust
- **F1 — Map/list filters + search** (connector, speed, available-now, network, price) and text search — today only on the detail screen.
- **F2 — Ratings, reviews, photos, check-ins** (write path) — PlugShare's entire moat; also fixes the dead `Station.rating` input to recommendations.
- **F3 — Favorites & recents** — saved stations + recent-from-history; pure retention win for commuters and fleets.

### G. Booking Depth
- **G1 — Reserve-ahead / scheduled bookings** beyond the instant 20-min hold.
- **G2 — Recurring bookings** (commuter/fleet templates) — already a README roadmap commitment.

### H. Account, Compliance & Security
- **H1 — Account deletion + data export.** **Google Play hard requirement** for account-based apps, and a DPDP Act 2023 right. Currently no path in app or API — a **store-listing blocker**.
- **H2 — Biometric app-lock** for a wallet/payment-bearing app.
- **H3 — Deep links / App Links** — required to make notification taps, shared station links, and emailed receipts land in the right screen.

### I. Quality & Observability
- **I1 — Crash reporting + product analytics** (funnel: discover→book→charge→pay). You cannot operate a launch blind.
- **I2 — Accessibility pass** (TalkBack on the booking→charging→payment funnel; `semantics{}` on custom Clay/telemetry composables).
- **I3 — Localization (i18n)** — externalize strings; Hindi first for the truck segment.
- **I4 — Architecture hardening** — introduce **Hilt** + a repository layer and split the monolithic `MainActivity` nav graph before feature volume makes it unmaintainable. (Enabler, not user-facing, but it gates the velocity of everything above.)

> **Upstream dependency (not an Android task, but it gates launch):** the backend currently **generates OTPs but never sends an SMS** (flagged in the system audit as #1). No mobile feature matters until a real user can receive a login code. The Android team should treat this as a launch precondition owned by backend.

---

## Section 3: Prioritized Implementation Roadmap

Priority key: **High** = required for a credible commercial launch (blocker, trust, or core-experience). **Medium** = strongly expected by users / drives retention; fast-follow. **Low** = differentiating or polish; post-launch.

| Feature | Priority | Business / User Impact | Architectural / Technical Notes (Compose + Retrofit + StateFlow) |
|---|---|---|---|
| **A1 — Background charging foreground service** | **High** | The core promise ("watch your charge") breaks when the screen sleeps; users see a dead session and distrust billing. | Foreground `Service` (manifest `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` on API 34). Move `StompClient` ownership out of `ChargingViewModel`/Activity into the service; expose session state as a `StateFlow` the `ChargingScreen` collects via a bound connection or a repository singleton. Ongoing notification mirrors SoC + Stop action. |
| **A3 — Persist rotated tokens** | **High** | Silent logouts / extra 401 round-trips after every app restart; worsens if backend rotates refresh tokens. | In `RetrofitClient`'s `Authenticator`, on success call back into `UserPreferencesRepository.saveAuthToken/saveRefreshToken` (inject a persistence callback to keep `RetrofitClient` a pure singleton). ~Half-day fix, high ROI. |
| **B1 — Push notifications (FCM)** | **High** | A driver who walks away never learns charging finished or that the hold expired — direct churn + billing disputes. | Add `google-services` + `firebase-messaging`; `FirebaseMessagingService` for tokens/data messages; register token on login via a new `POST /api/users/device-token`. Notification taps route through deep links (H3). Backend trigger points already exist. |
| **H1 — Account deletion + data export** | **High** | **Google Play will reject/limit the listing** without an in-app deletion path; also DPDP compliance. | `DELETE /api/users/me` (soft-delete + PII anonymization, retain financial rows). Surface in `ProfileScreen` with confirm dialog; also a public web URL for the Play Console field. |
| **I1 — Crash reporting + analytics** | **High** | Launching without crash/funnel visibility means the first outage is a user complaint; no data to prioritize. | Crashlytics (rides on the Firebase setup from B1) + an analytics wrapper behind an interface; instrument the discover→book→charge→pay funnel. Minimal code, must precede public traffic. |
| **D2 — Receipts / GST invoices (in-app)** | **High** | Table-stakes in India; the **truck/fleet** segment cannot expense charging without GST invoices. | Backend generates PDF; app adds "Download/Share invoice" on `PaymentSuccessScreen` + history rows (Coil/`FileProvider` or a download intent). |
| **A2 — Offline cache (Room)** | **Medium** | The moment of need (standing at a charger, poor signal) is exactly when a network-only app fails. | Add Room; repositories return cached-then-fresh `Flow`s (single source of truth) feeding existing `StateFlow`s. Start with stations + active booking + history. |
| **A4 — Graceful global sign-out** | **Medium** | A truly-expired session currently produces failed calls with no redirect — confusing dead-ends. | Emit a `SharedFlow<AuthEvent>` from the repository on terminal refresh failure; collect in `EVChargingApp` to navigate to `login` and clear state. |
| **C1 — Vehicle garage** | **Medium** | Accurate remaining-time, auto connector selection, and the foundation for route planning; reduces booking friction. | `Vehicle` model + `api/users/{id}/vehicles` CRUD; `VehicleViewModel` (`StateFlow`); selected vehicle pre-fills `SlotBookingScreen`. |
| **D1 — Saved payment / prepaid wallet** | **Medium** | Removes per-session checkout friction; wallet + auto-reload is the dominant Indian pattern (Statiq/Kazam). | Razorpay tokenization or a backend wallet ledger; new endpoints + a `WalletViewModel`; balance chip on Home/checkout. Pairs with D4 auto-charge. |
| **E1 — Navigation hand-off** | **Medium** | Getting to the charger is half the job; a missing "Directions" button is a conspicuous gap vs. every competitor. | "Directions" button on `StationDetailScreen` firing a `google.navigation:q=lat,lng` intent. No backend. Low effort, high visibility. |
| **F2 — Ratings & reviews (write)** | **Medium** | Trust signal that drives station choice; also feeds the currently-dead `Station.rating` recommendation input. | `Review` entity gated on a `COMPLETED` session; `POST` endpoint; review list + composer in `StationDetailScreen`; recompute rating on write. |
| **F3 — Favorites & recents** | **Medium** | Repeat users (commuters, fleets) re-search every time; cheap retention lever. | `user_favorite_station` join + heart on `StationCard`/detail; "Recents" derived from booking history (no schema). |
| **F1 — Discovery filters + search** | **Medium** | Users can't narrow the map to "DC, available now"; forces tap-through on every pin. | Lift the detail filter chips to Home; add a `StateFlow<FilterState>` in `StationViewModel`; debounce text search against name/address. |
| **C2/C3 — Editable profile + settings hub** | **Medium** | Read-only profile feels unfinished; settings is where notif prefs (B2), theme, language, support live. | `PUT api/users/{id}`; convert `ProfileScreen` to stateful form; add a `SettingsScreen` route in the `Screen` sealed graph. |
| **H2 — Biometric app-lock** | **Medium** | Protects a wallet/payment-bearing app on shared/lost devices. | `androidx.biometric`; gate launch + payment actions; toggle in settings (C3). |
| **H3 — Deep links / App Links** | **Medium** | Prerequisite for B1 notification taps, shared-station links, and D2 emailed receipts to open the right screen. | Add `nav-graph` deep links + verified App Links intent-filters mapped onto existing `Screen` routes. |
| **G1 — Reserve-ahead / scheduled booking** | **Low** | Removes the "only book when standing there" limitation; planning for trips/fleets. | Extend `BookingRequest` with a start time; backend slot-availability over a window; date/time picker in `SlotBookingScreen`. |
| **E2 — Trip / corridor route planning** | **Low** | Signature differentiator (PlugShare/ABRP), high value for inter-city trucking — but only after the basics work. | Phase 1: stations within X km of a Directions polyline (needs C1 vehicle range). Defer SoC-aware stop optimization. |
| **D3 — Promo / referral / loyalty** | **Low** | Growth/retention lever; meaningful only once the paid funnel is solid. | Promo field → backend validation at intent creation; referral share via `ACTION_SEND`; points ledger later. |
| **G2 — Recurring bookings** | **Low** | Roadmap commitment; commuter/fleet convenience. | `BookingTemplate` materialized T-24h by the existing scheduler; app just manages templates. |
| **I3 — Localization (Hindi)** | **Low** | Inclusivity for the truck segment; cheap to scaffold now, costly to retrofit later. | Externalize strings to resource qualifiers now; translate Hindi first. |
| **I2 — Accessibility pass** | **Low** | Foundation exists (~42 `contentDescription`s); needs a TalkBack audit, not a rebuild. | Accessibility Scanner + TalkBack on the core funnel; `semantics{}` on custom telemetry/Clay composables. |
| **I4 — Hilt + repository refactor** | **Low (enabler)** | Not user-facing, but `MainActivity` (~550 lines, hand-wired VMs) will throttle delivery of everything above. | Introduce Hilt; extract repositories; split the nav graph. Do it *alongside* A1/A2 since both restructure data ownership. |
| **iOS / cross-platform** | **Won't (this release)** | Real demand exists, but it's a major investment; validate the funnel on Android first. | Later: Compose Multiplatform / KMP leverages the existing Kotlin layer; or a mobile-web driver flow on the React stack. |

---

## Section 4: Priority Rationale

**What "High" means here: a paying stranger can complete the loop, trust it, and you can support it.** Four lenses decide the cut.

**1. A user must be able to charge end-to-end *and trust the result* — even when they put the phone in their pocket.**
The defining act of this app is the live charging session, and today it is the most fragile path: the socket and session live in-process with **no foreground service**, so backgrounding or Doze kills telemetry, and process death loses the session. That is why **A1 (foreground service)** outranks every additive feature — a charging app that can't reliably show a charge in progress isn't shippable. **B1 (push)** is its complement: the realistic behavior is to walk away from the car, and without push the user never learns the charge finished or the hold expired. **A3 (persist tokens)** is a small but real correctness bug that erodes trust through silent logouts, so it earns High despite low effort.

**2. The store and the law gate the listing regardless of feature richness.**
**H1 (account deletion)** is a Google Play **publishing requirement** for account-based apps — no amount of polish ships without it, so it's High purely on gating. **D2 (GST invoices)** is similarly non-negotiable for the explicitly-targeted truck/fleet segment, who cannot expense charging without them.

**3. You cannot operate a launch you cannot see.**
**I1 (crash + analytics)** is High not for users but for the team: the first production crash and the first funnel drop-off must be observable, or every subsequent priority is guesswork. It's cheap and must land before public traffic.

**4. Why the genuinely attractive features are deliberately *not* High.**
Route planning (**E2**), loyalty (**D3**), and recurring bookings (**G2**) are the features that demo best and differentiate most — and they are exactly where pre-launch teams over-invest. They are sequenced **Low** on purpose: they multiply the value of a working core, but they cannot substitute for one. Building corridor planning on top of a session that drops when the screen sleeps just adds surface area to an unreliable base.

**The Medium tier is the retention engine that turns a working app into a sticky one** — vehicle garage, wallet, navigation hand-off, reviews, favorites, filters. None are launch-blocking, but each materially reduces friction or increases repeat use, and several unblock the Low tier (vehicle range → route planning; deep links → push taps and receipts; wallet → auto-charge).

**Suggested mobile sequencing (consistent with the system-wide audit's sprints):**

| Phase | Items | Theme |
|---|---|---|
| **Phase 1 — Make it shippable** | A1, A3, B1, H1, I1, D2 *(+ backend SMS-OTP precondition)* | Reliable core experience + store/legal/observability gates |
| **Phase 2 — Make it sticky** | A2, A4, C1, D1, E1, F1, F2, F3, C2/C3, H2, H3 | Offline resilience + retention loop |
| **Phase 3 — Make it differentiated** | G1, E2, D3, G2, I3, I2 | Planning, growth, inclusivity |
| **Continuous** | I4 (Hilt/repo refactor) — fold into Phase 1/2 since A1/A2 already restructure data ownership | Velocity enabler |

**One-line bottom line:** the app has an impressively complete *happy-path*; the gap to commercial launch is **reliability under real-world conditions (background, offline, token rotation), the store/legal gates (deletion, invoices), and operational visibility (crash/analytics)** — not more features.
