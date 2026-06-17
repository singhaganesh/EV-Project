# Android Driver App — Product Audit & Production Roadmap ("Plugsy")

**Author lens:** Mobile Product Manager + Principal Android Architect
**Original audit:** 2026-06-16 · **Last updated:** 2026-06-17 (re-audited against source)
**Scope:** The **consumer/driver Android app** only (`android/`, package `com.ganesh.ev`). The React owner/admin dashboard and Spring Boot backend are referenced only where the mobile experience depends on them.
**App state:** `versionCode 1` / `versionName 1.0`, `minSdk 24`, `targetSdk 35`, release base URL `https://api.plugsy.in/`. Pre-first-release — but the reliability/store-gate layer that the original audit called the gap to launch is now largely built (see Update Log).
**Benchmarks:** ChargePoint, EVgo, Electrify America, PlugShare (global); Statiq, Kazam, Tata Power EZ Charge, ChargeZone (India, the actual competitive set).

> **Relationship to the existing audit.** A full-stack `plans/pending/missing-features-audit.md` already exists (system-wide, 2026-06-12). This document does **not** replace it — it goes deeper on the *mobile client* with concrete Compose/Retrofit/StateFlow integration notes, and isolates Android-only concerns (background charging service, offline cache, on-device token persistence, deep links). Where priorities overlap, they are kept consistent with that document.

---

## Update Log — what changed since the 2026-06-16 audit

The original audit concluded the gap to launch was **reliability under real-world conditions, store/legal gates, and observability — not more features.** Since then, most of that gap has been closed. Verified ✅ against current source:

| Original item | Status now | Where (verified) |
|---|---|---|
| **Backend SMS-OTP precondition** (was the #1 launch blocker — backend generated OTP but never sent SMS) | ✅ **Resolved a different way** — replaced custom SMS OTP with **Firebase Phone Auth** (Google sends/verifies the SMS; **no India DLT registration needed**). App exchanges the Firebase ID token at `POST /api/auth/firebase-login` for the local JWT. | `AuthViewModel.kt`, `LoginScreen.kt`, backend `AuthController.firebaseLogin`, `FirebaseAuthService` |
| **A1 — Background charging foreground service** | ✅ **Done** | `service/ChargingManager.kt` (process-singleton socket), `service/ChargingForegroundService.kt` (ongoing SoC notification + Stop action, `START_REDELIVER_INTENT`), manifest `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` |
| **A3 — Persist rotated tokens** | ✅ **Done** | `EvApplication.kt:42` `RetrofitClient.setTokenPersister { … }` writes refreshed access/refresh tokens back to DataStore |
| **B1 — Push (FCM)** | ✅ **Done** | `data/notifications/EvMessagingService.kt`, `Notifications.kt` (3 channels), `DeviceTokenRegistrar.kt`, `POST /api/users/device-token` |
| **H1 — Account deletion** | ✅ **Done** | `ProfileScreen.kt` delete dialog → `ProfileViewModel.deleteAccount()` → `DELETE /api/users/me` (PII anonymized, financial rows retained) |
| **H3 — Deep links / App Links** | ✅ **Done** (App Links pending domain verification file) | Manifest `autoVerify` intent-filter for `https://plugsy.in/{charging,payment,bookings,station}` + `plugsy://` scheme; nav `deepLinks` in `MainActivity.kt` |
| **I1 — Crash reporting + analytics** | ✅ **Done** | `firebase-crashlytics` + `firebase-analytics` deps; `util/AppAnalytics.kt` funnel (`app_open → station_view → booking_created → charging_started → charging_completed → payment_success`), instrumented in `ChargingViewModel`/`EvApplication` |
| **C2 — Editable profile** | ✅ **Done** | `ProfileScreen.kt` inline name/email edit → `PUT /api/users/{id}` |
| **C3 — Settings hub** | ⚠️ **Partial** | `SettingsScreen.kt`: notifications toggle, "follows system theme" (no real theme switch), about/version, static support email |
| **E1 — Navigation hand-off** | ✅ **Done** | `ui/components/StationCard.kt:227` fires `google.navigation:q=…`; helper in `util/LocationHelper.kt` |
| **A2 — Offline cache (Room)** | ⚠️ **Partial** | Room added (`data/local/*`), but caches **stations only** (`StationCache`/`StationDao`); read-path fallback in `StationViewModel.emitCachedOrError`. No active-booking/history cache; **no WorkManager**. |
| **I4 — Hilt + repo refactor** | ⚠️ **Partial** | Hilt wired (`@HiltAndroidApp EvApplication`, `di/AppModule.kt`, `@HiltViewModel ProfileViewModel`, `hiltViewModel()`), but `MainActivity.kt:54-55` still **hand-constructs** `ChargingViewModel` + `UserPreferencesRepository`; most VMs still use plain `viewModel()`; nav graph still monolithic (~590 lines). |
| **STOMP heartbeat `0,0`** | ✅ **Mitigated** | `StompClient.kt:31` OkHttp `pingInterval(20s)` surfaces a dead peer as `onFailure` → reconnect. |

**Net effect:** of the original **High** tier (A1, A3, B1, H1, I1, D2), **five of six are done**; only **D2 (GST invoices)** remains. The launch-blocking work is now narrow.

---

## Section 1: Detailed List of Current Features (Audited)

Each item verified **against source**. ✅ = present and wired; ⚠️ = present but with a caveat that affects production.

### 1.1 Authentication & Session
- ✅ **Phone login via Firebase Phone Auth** (`AuthViewModel.kt`, `LoginScreen.kt`). Flow: `PhoneAuthProvider` send/resend/verify (incl. instant verification) → Firebase ID token → `POST /api/auth/firebase-login` exchanges it for the local JWT + refresh token. New users are created server-side immediately (phone is verified) and then set **Name/Email** via the authenticated `PUT /api/users/{id}` (the old `complete-profile` / `CompleteProfileRequest` were removed).
- ✅ **Login UX hardened** — `+91` prefix + strict 10-digit validation (`^[6-9]\d{9}$`), **WhatsApp-style "verify this number?" confirm dialog**, segmented 6-box OTP input (auto-advance/backspace), **60s resend countdown**, auto-submit at 6 digits, and **SMS User Consent API** one-tap autofill of the incoming code. The debug "Your OTP" card was removed.
- ✅ **Token lifecycle** — access + refresh tokens persisted in DataStore (`UserPreferencesRepository`); auto-login via `SplashScreen` (`onAuthValid` / `onAuthExpired`).
- ✅ **Silent 401 recovery** — `RetrofitClient` registers an OkHttp `Authenticator` that calls `refresh-token` on a separate client (no interceptor recursion) and retries, capped at 3 attempts.
- ✅ **Token-rotation persistence (was a bug, now fixed)** — `EvApplication` hands `RetrofitClient` a persister callback so a token refreshed by the `Authenticator` is written back to DataStore and survives a process restart.
- ⚠️ **Rate limiting is Firebase-side only** — the backend `firebase-login` endpoint has no throttle (it only verifies a token). Abuse protection for SMS sends relies entirely on Firebase's (adaptive, undocumented) anti-abuse. Acceptable, but worth noting for cost control.
- ⚠️ **Release-signing fingerprint not yet registered** — Phone Auth will fail in a Play-Store release build until the **release keystore SHA-1/SHA-256** is added in Firebase (debug fingerprints are registered; no release keystore exists yet).

### 1.2 Onboarding
- ✅ **4-page slider** (`OnboardingScreen`), gated by a DataStore `should_show_onboarding` flag, shown once before login.
- ⚠️ **`clearUserData()` wipes the onboarding flag too** (`UserPreferencesRepository.kt:77` `preferences.clear()`), so a logout re-shows onboarding on next launch. Minor, but a visible regression on logout.

### 1.3 Map / Discovery (`HomeScreen`)
- ✅ Google Maps Compose with custom pins; **viewport-debounced** fetching via `getViewportWithNearby` (full data for top-N, lightweight pins for the rest); 15%-delta refetch guard.
- ✅ List/map toggle, **distance sort**, relative "last used" time, bottom station pager.
- ✅ **Offline fallback** — when the network fails, `StationViewModel.emitCachedOrError` serves the last Room-cached stations instead of a blank screen (CV-10).
- ✅ **Directions hand-off** — `StationCard` fires a `google.navigation:q=lat,lng` intent.
- ⚠️ **No discovery-level filtering or text search** — connector/speed/"available now"/network filters and search-by-name/address still don't exist at the map/list level (`HomeScreen`/`StationViewModel` have no filter state). *(Gap F1.)*

### 1.4 Station Details (`StationDetailScreen`)
- ✅ Operating-hours parsing with live open/closed computation; guns grouped by dispensary unit; tabs **Charger / Details / Reviews**.
- ✅ Filter chips (**Available / AC / DC**) on the detail screen; **static** map preview.
- ✅ Live grid metrics (Voltage, Current, Power, Forecasted Load) via `getStationLivePower`; weighted scores (Traffic, Grid, Parking, Accessibility) via `getStationDetail`.
- ⚠️ **Reviews tab is a placeholder** — `ReviewsTabContent()` (`StationDetailScreen.kt:~981`) renders "No reviews yet / Be the first to review this station" with **no read or write path**. `Station.rating` remains display-only with nothing populating it. *(Gap F2.)*

### 1.5 Slot Booking (`SlotBookingScreen` + shared `BookingViewModel`)
- ✅ Instant **20-minute reservation**, connector selection (CCS2 / Type-2), vehicle class (Car / Truck), and the **truck-fallback** prompt when car slots are exhausted.
- ⚠️ **Instant-only** — no reserve-ahead / scheduled / recurring booking. *(Gaps G1/G2.)*

### 1.6 Booking Management
- ✅ Active-reservation **countdown timer**, booking detail with **cancel**, paginated history (`getUserBookings`, page/size).

### 1.7 Real-Time Charging (`ChargingScreen` + `ChargingViewModel` + `ChargingManager` + `StompClient`)
- ✅ STOMP-over-WebSocket telemetry: live **SoC%** (pulsing ring), power, elapsed cost, energy, remaining time, **emergency stop**.
- ✅ **Background survival (was the #1 experiential risk, now fixed)** — the socket lives in the process-singleton `ChargingManager`, and `ChargingForegroundService` (type `dataSync`) keeps it alive with an **ongoing SoC notification + Stop action**. `ChargingViewModel` merely mirrors `ChargingManager.telemetry`; `onCleared` detaches the UI but **leaves the service running**. `START_REDELIVER_INTENT` + `ChargingManager.resume()` recover the session after process death. The charging notification deep-links back into the live screen (`?isNewSession=false`).
- ✅ **Resilient socket** — exponential-backoff reconnect (2s→30s) with topic re-subscribe; JWT in the CONNECT frame; OkHttp `pingInterval(20s)` detects half-open connections (mitigates the old `heart-beat:0,0`).
- ⚠️ **Error-recovery is heuristic** — `ChargingScreen` inspects error message text ("already exists", "session not found") to decide whether to start vs. resume. Works, but brittle to backend message changes.

### 1.8 Payment & Checkout
- ✅ Invoice summary, **Razorpay** checkout (card/UPI), server-side **signature verification** (`payments/verify`), success screen. `MainActivity` implements `PaymentResultWithDataListener` and routes the result into the shared `ChargingViewModel`.
- ⚠️ **No stored payment instrument, wallet, or receipt artifact** — Razorpay is re-entered every session; no downloadable/emailed receipt or **GST invoice**. *(Gaps D1/D2.)*

### 1.9 Profile (`ProfileScreen` + `ProfileViewModel`)
- ✅ **Editable** name/email (inline edit → `PUT /api/users/{id}`), shows mobile/role/member-since/last-updated, **Logout**, **Settings** entry, and **Delete Account** (confirm dialog → `DELETE /api/users/me`).
- ⚠️ **No vehicle garage, saved payment methods, profile photo, or per-category notification prefs.** *(Gaps C1, D1, B2.)*

### 1.10 Architecture baseline
- ✅ MVVM + `StateFlow`; Navigation-Compose with a single sealed `Screen` graph; Material 3 + custom "Clay" theme; Coil; **Hilt** DI; **Room**; **Firebase** (Auth/Messaging/Analytics/Crashlytics).
- ⚠️ **Hilt only partially adopted** — `MainActivity.kt:54-55` still hand-wires `ChargingViewModel` + `UserPreferencesRepository`; most VMs use plain `viewModel()`/manual factories rather than injection; no repository layer abstracts `RetrofitClient`; the nav graph is still a ~590-line monolith. *(Gap I4.)*
- ⚠️ **Room is minimal** — single `StationEntity`; no offline cache for active booking or history; **no WorkManager** for durable background retry. *(Gap A2.)*
- ⚠️ **No graceful global sign-out** — when refresh ultimately fails, the `Authenticator` returns `null` with no app-level reaction; only the next `SplashScreen` launch routes to login. *(Gap A4.)*
- ⚠️ **Notifications toggle not enforced** — `SettingsScreen`'s `notificationsEnabled` pref is stored but **not consulted** by `Notifications.show`/`EvMessagingService`; toggling it off does not actually suppress pushes. *(Gap B2.)*

---

## Section 2: Categorized Gaps & Missing Features

Status tags reflect the 2026-06-17 re-audit: **[DONE]**, **[PARTIAL]**, **[OPEN]**.

### A. Reliability, Background & Offline
- **A1 — Background charging foreground service.** **[DONE]** `ChargingManager` + `ChargingForegroundService`.
- **A2 — Offline cache (Room).** **[PARTIAL]** Stations cached + served offline; **still missing**: active booking + history offline, and **WorkManager** for durable retry (e.g. device-token registration, payment verification retries).
- **A3 — Persist rotated tokens.** **[DONE]** `EvApplication.setTokenPersister`.
- **A4 — Graceful global sign-out.** **[OPEN]** Emit a `SharedFlow<AuthEvent>` from the network/repository layer on terminal refresh failure; collect in `EVChargingApp` to navigate to `login` and clear state. Today a truly-expired session just yields failing calls until the next cold start.

### B. Notifications & Alerts
- **B1 — Push (FCM).** **[DONE]** Token registration + typed channels + deep-linked taps; backend triggers exist (force-stop, expiry scheduler, completion).
- **B2 — Notification preferences + runtime permission.** **[PARTIAL]** `POST_NOTIFICATIONS` is requested at first charging (`ChargingScreen`), and a **global** toggle exists — but the toggle is **not enforced** at display time, and there are **no per-category** toggles. Enforce the pref in `Notifications.show` and split per channel.

### C. Profile & Vehicle Management
- **C1 — Vehicle garage.** **[OPEN]** Make/model/**battery kWh**/preferred connector. Powers accurate "remaining time," auto-connector at booking, and is the prerequisite for route planning (E2).
- **C2 — Editable profile.** **[DONE]** (photo still absent — minor).
- **C3 — Settings hub.** **[PARTIAL]** Screen exists; **still missing**: real theme switch (currently "follows system" only), language/units, working Help/FAQ + contact action.

### D. Payment, Wallet & Loyalty
- **D1 — Saved payment / prepaid wallet.** **[OPEN]** Tokenized card or prepaid wallet + auto-reload (dominant Statiq/Kazam pattern).
- **D2 — Receipts & GST invoices.** **[OPEN]** Still the **single remaining High-tier launch gap** for the truck/fleet segment.
- **D3 — Promo / referral / loyalty.** **[OPEN]**
- **D4 — Auto-charge on session end** (post-paid). **[OPEN]** Depends on D1.

### E. Navigation & Route Planning
- **E1 — Turn-by-turn hand-off.** **[DONE]** `google.navigation:` intent from `StationCard`. (Could also surface on `StationDetailScreen` for parity.)
- **E2 — Corridor / trip planning.** **[OPEN]** Depends on C1 (vehicle range).

### F. Discovery & Social Trust
- **F1 — Map/list filters + search.** **[OPEN]** Lift the detail filter chips to Home; add `StateFlow<FilterState>` + debounced text search.
- **F2 — Ratings, reviews, photos.** **[OPEN]** Tab scaffolded but empty; needs a `Review` entity gated on a `COMPLETED` session, `POST`/list endpoints, composer in the Reviews tab, and recompute of `Station.rating`.
- **F3 — Favorites & recents.** **[OPEN]**

### G. Booking Depth
- **G1 — Reserve-ahead / scheduled bookings.** **[OPEN]**
- **G2 — Recurring bookings.** **[OPEN]**

### H. Account, Compliance & Security
- **H1 — Account deletion + data export.** **[PARTIAL → mostly DONE]** In-app deletion shipped (Play hard requirement satisfied). **Still needed**: a **public web deletion URL** for the Play Console field, and a **data-export** path (DPDP right).
- **H2 — Biometric app-lock.** **[OPEN]** `androidx.biometric`; gate launch + payment.
- **H3 — Deep links / App Links.** **[DONE, with one hosting step]** Intent-filters + nav deep links wired; **App Links `https` auto-verification requires hosting `https://plugsy.in/.well-known/assetlinks.json`** — until then only the `plugsy://` scheme reliably resolves.

### I. Quality & Observability
- **I1 — Crash reporting + analytics.** **[DONE]** Crashlytics + Analytics funnel instrumented.
- **I2 — Accessibility pass.** **[OPEN]** TalkBack on the booking→charging→payment funnel; `semantics{}` on Clay/telemetry composables (incl. the new segmented OTP boxes).
- **I3 — Localization (i18n).** **[OPEN]** Strings still hard-coded; Hindi first for the truck segment.
- **I4 — Architecture hardening.** **[PARTIAL]** Hilt present but not fully adopted; extract a repository layer behind `RetrofitClient`, inject `ChargingViewModel`/`UserPreferencesRepository`, and split the `MainActivity` nav graph.

---

## Section 3: Prioritized Implementation Roadmap (revised 2026-06-17)

Priority key: **High** = required for a credible commercial launch. **Medium** = retention/expectation; fast-follow. **Low** = differentiating/polish. **✅ Done / ⚠️ Partial** items are kept for traceability.

| Feature | Status | Priority | Business / User Impact | Technical Notes (Compose + Retrofit + StateFlow) |
|---|---|---|---|---|
| A1 — Background charging service | ✅ Done | — | Core "watch your charge" promise now survives backgrounding/Doze. | `ChargingManager` + `ChargingForegroundService`. |
| A3 — Persist rotated tokens | ✅ Done | — | No more silent logouts after restart. | `EvApplication.setTokenPersister`. |
| B1 — Push (FCM) | ✅ Done | — | Users learn charge-complete / hold-expiry / force-stop. | `EvMessagingService` + channels + device-token. |
| H1 — Account deletion | ✅ Done | — | Satisfies Google Play deletion requirement (in-app). | `DELETE /api/users/me`. *Still add public web URL + export → see H1 row below.* |
| I1 — Crash + analytics | ✅ Done | — | Launch is observable (funnel + crashes). | Crashlytics + `AppAnalytics`. |
| Auth → Firebase Phone Auth | ✅ Done | — | Removes the old "OTP never sent" blocker; no DLT. | `firebase-login` token exchange. |
| **D2 — Receipts / GST invoices (in-app)** | **[OPEN]** | **High** | The **last** launch blocker: truck/fleet cannot expense charging without GST invoices. | Backend generates PDF; "Download/Share invoice" on `PaymentSuccessScreen` + history rows via `FileProvider`/download intent. |
| **H1b — Public deletion URL + data export** | **[OPEN]** | **High** | Play Console requires a **web** deletion URL even with in-app deletion; DPDP export right. | Static web page hitting the same delete flow; `GET /api/users/me/export` (JSON/email). |
| **H3b — Host `assetlinks.json`** | **[OPEN]** | **High (small)** | Without it, `https://plugsy.in` App Links don't auto-verify → notification/shared links may not open the app. | Publish `https://plugsy.in/.well-known/assetlinks.json` with the app's signing cert SHA-256. |
| **Release-keystore SHA in Firebase** | **[OPEN]** | **High (small)** | Phone Auth **fails in the Play release build** until the release signing fingerprint is registered. | Create release keystore → add SHA-1/SHA-256 in Firebase → re-download `google-services.json`. |
| A2 — Offline cache (booking/history) + WorkManager | ⚠️ Partial | **Medium** | Stations cache exists; booking/history offline + durable retries still missing at the point of need. | Extend Room beyond `StationEntity`; add WorkManager for device-token + payment-verify retries. |
| A4 — Graceful global sign-out | [OPEN] | **Medium** | Expired sessions currently dead-end until cold start. | `SharedFlow<AuthEvent>` on terminal refresh failure → navigate `login`. |
| B2 — Enforce notif pref + per-category toggles | ⚠️ Partial | **Medium** | Toggling notifications off does nothing today; trust/ápp-store hygiene. | Consult the pref in `Notifications.show`; per-channel switches in `SettingsScreen`. |
| C1 — Vehicle garage | [OPEN] | **Medium** | Accurate remaining-time, auto-connector, foundation for route planning. | `Vehicle` CRUD + `VehicleViewModel`; pre-fill `SlotBookingScreen`. |
| D1 — Saved payment / wallet | [OPEN] | **Medium** | Removes per-session checkout friction (dominant India pattern). | Razorpay tokenization or backend wallet; `WalletViewModel`; balance chip. |
| F1 — Discovery filters + search | [OPEN] | **Medium** | Can't narrow map to "DC, available now"; forces tap-through. | `StateFlow<FilterState>` in `StationViewModel`; debounced name/address search. |
| F2 — Ratings & reviews (write) | [OPEN] (tab stubbed) | **Medium** | Trust signal; also feeds the dead `Station.rating`. | `Review` gated on `COMPLETED`; `POST`/list; fill the existing Reviews tab. |
| F3 — Favorites & recents | [OPEN] | **Medium** | Cheap retention for commuters/fleets. | `user_favorite_station` + heart; recents from history. |
| C3b — Real theme/language + working Help | ⚠️ Partial | **Medium** | Settings looks unfinished (placeholders). | Theme `StateFlow` in DataStore; string resources (ties to I3); Help → FAQ/contact intents. |
| H2 — Biometric app-lock | [OPEN] | **Medium** | Protects a payment-bearing app. | `androidx.biometric`; gate launch + payment. |
| I4 — Finish Hilt + repository refactor | ⚠️ Partial | **Medium (enabler)** | Hand-wired VMs + 590-line `MainActivity` throttle feature velocity. | Inject `ChargingViewModel`/prefs; repository layer over `RetrofitClient`; split nav graph. |
| G1 — Reserve-ahead / scheduled booking | [OPEN] | **Low** | Removes "only book when standing there." | `BookingRequest` + start time; window availability; date/time picker. |
| E2 — Trip / corridor route planning | [OPEN] | **Low** | Signature differentiator; needs C1 range first. | Stations within X km of a Directions polyline. |
| D3 — Promo / referral / loyalty | [OPEN] | **Low** | Growth lever once the paid funnel is solid. | Promo at intent creation; referral via `ACTION_SEND`. |
| G2 — Recurring bookings | [OPEN] | **Low** | Commuter/fleet convenience. | `BookingTemplate` materialized by the scheduler. |
| I3 — Localization (Hindi) | [OPEN] | **Low** | Inclusivity for trucking; cheap now, costly later. | Externalize strings; translate Hindi first. |
| I2 — Accessibility pass | [OPEN] | **Low** | Foundation exists; needs a TalkBack audit. | Scanner + `semantics{}` on telemetry/OTP composables. |
| iOS / cross-platform | [OPEN] | Won't (this release) | Validate the funnel on Android first. | Later: KMP / mobile-web driver flow. |

---

## Section 4: Priority Rationale (revised)

**The original "make it shippable" tier is essentially built.** A paying stranger can now log in (Firebase Phone Auth, no DLT), discover/book/charge with the session surviving a backgrounded screen (foreground service), get told when it finishes (FCM), delete their account in-app (Play gate), and the team can see crashes and the funnel (Crashlytics + Analytics). That is a different posture than the 2026-06-16 audit, which correctly identified those as the launch gap.

**What actually still blocks a clean store launch is now small and concrete:**
1. **D2 — GST invoices.** The one remaining feature-level launch gap for the explicitly-targeted truck/fleet segment.
2. **H1b / H3b / release-SHA — the "paperwork" gates.** A public deletion URL for the Play Console field, the hosted `assetlinks.json` so `https` App Links verify, and the **release-keystore fingerprint in Firebase** (without which Phone Auth breaks the moment you ship a signed release). These are low-effort but each is independently capable of blocking or breaking launch, so they are **High**.

**The Medium tier is unchanged in spirit but now the real frontier:** finish the partials before adding new surface area — extend offline cache + WorkManager (A2), enforce/segment notifications (B2), graceful sign-out (A4), and complete the Hilt/repository refactor (I4) so the codebase can absorb the retention features (C1 vehicles, D1 wallet, F1 filters, F2 reviews, F3 favorites) without `MainActivity` becoming unmanageable.

**The Low tier (route planning, loyalty, recurring, i18n, a11y) stays deliberately last** — they demo best and differentiate most, which is exactly why pre-launch teams over-invest in them. They multiply the value of a working core; they cannot substitute for closing the four small gates above.

**Suggested mobile sequencing (revised):**

| Phase | Items | Theme |
|---|---|---|
| **Phase 1 — Close the launch gates** | D2, H1b (web URL + export), H3b (assetlinks.json), release-keystore SHA | The last feature gate + store/legal/release paperwork |
| **Phase 2 — Finish the partials, then retain** | A2 (booking/history + WorkManager), A4, B2, I4, then C1, D1, F1, F2, F3, C3b, H2 | Robustness + retention loop |
| **Phase 3 — Differentiate** | G1, E2, D3, G2, I3, I2 | Planning, growth, inclusivity |

**One-line bottom line:** the original audit's launch gap (reliability, store gates, observability) is **mostly closed**; what remains for a clean launch is **GST invoices plus three small store/release "paperwork" gates**, after which the work shifts decisively to **finishing the half-built robustness layer and the retention features.**
