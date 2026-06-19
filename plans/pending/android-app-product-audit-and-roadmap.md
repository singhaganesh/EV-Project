# Android Driver App — Product Audit & Production Roadmap ("Plugsy")

**Author lens:** Mobile Product Manager + Principal Android Architect
**Original audit:** 2026-06-16 · **Last updated:** 2026-06-19 (re-audited against source, HEAD `bc9f142`)
**Scope:** The **consumer/driver Android app** only (`android/`, package `com.ganesh.ev`). The React owner/admin dashboard and Spring Boot backend are referenced only where the mobile experience depends on them.
**App state:** `versionCode 1` / `versionName 1.0`, `minSdk 24`, `targetSdk 35` (`compileSdk 35`), release base URL `https://api.plugsy.in/`. Pre-first-release — but the reliability/store-gate layer the original audit called the gap to launch is now largely built, **and most of the Medium/Low retention roadmap shipped on 2026-06-17–19** (see Update Log).
**Benchmarks:** ChargePoint, EVgo, Electrify America, PlugShare (global); Statiq, Kazam, Tata Power EZ Charge, ChargeZone (India, the actual competitive set).

> **Relationship to the existing audit.** A full-stack `plans/pending/missing-features-audit.md` already exists (system-wide, 2026-06-12). This document does **not** replace it — it goes deeper on the *mobile client* with concrete Compose/Retrofit/StateFlow integration notes, and isolates Android-only concerns (background charging service, server-authoritative completion, offline cache, on-device token persistence, deep links). Where priorities overlap, they are kept consistent with that document.

---

## Update Log

### Wave 2 — what changed 2026-06-17 → 2026-06-19 (this re-audit)

After the 2026-06-17 audit was written that morning, **almost the entire Medium/Low roadmap was implemented the same day**, followed by a **server-authoritative charging-completion overhaul** and **auth/OTP hardening** on 06-18–19. Verified ✅ against current source:

| Roadmap item (prior status) | Status now | Where (verified) |
|---|---|---|
| **A2 — offline booking/history + WorkManager** (was Partial) | ✅ **Done** | `data/local/JsonStore.kt` + `JsonCacheEntity`/`JsonCacheDao` (generic JSON cache beyond stations); `data/notifications/DeviceTokenWorker.kt` + `DeviceTokenRegistrar.enqueue` (WorkManager-backed device-token retry) |
| **A4 — graceful global sign-out** (was Open) | ✅ **Done** | `data/network/SessionEvents.kt` (`SharedFlow<Unit> loggedOut`); `RetrofitClient` authenticator emits on terminal refresh failure; `MainActivity.EVChargingApp` collects → clears state → `navigate("login")` |
| **B2 — enforce notif pref + per-category** (was Partial) | ✅ **Done** | `NotificationPrefs` (volatile mirror) consulted in `Notifications.show`; master + Charging/Reminders/Payments switches in `SettingsScreen`; `EvApplication` keeps the mirror synced |
| **C1 — vehicle garage** (was Open) | ⚠️ **Built, not wired** | `VehiclesScreen`/`VehicleViewModel`; `GET/POST/DELETE api/users/me/vehicles`; `Vehicle(make,model,batteryKwh,connectorType)`. **But `SlotBookingScreen` still uses a CAR/TRUCK toggle** — the garage doesn't yet pre-fill booking or feed remaining-time |
| **C3b — real theme switch** (was Partial) | ✅ **Done** | `Theme.kt` full light+dark `ColorScheme`; `MainActivity` reads `themeMode` pref → `EvTheme(darkTheme=…)`; System/Light/Dark chips in `SettingsScreen` |
| **D2 — receipts** (was Open / High) | ⚠️ **Receipt done, GST invoice open** | `service/ReceiptService.java` renders a **payment receipt PDF** (OpenPDF); `@Streaming GET api/payments/{id}/receipt`; `util/ReceiptHelper.kt` downloads + `FileProvider` share. Explicitly **not** a GST tax invoice (company not GST-registered) — layout pre-structured for GSTIN/HSN later |
| **E2 — route / trip planning** (was Open) | ✅ **Done** | `RoutePlanScreen` + `RoutePlanningViewModel` + `data/network/DirectionsApi.kt`; **Trip** added to the bottom nav |
| **F1 — discovery filters + search** (was Open) | ⚠️ **Partial** | `HomeScreen` has name/address **search** + **Available** + **Open now** filters (client-side over the loaded set; drives both map + list). **Still missing:** connector/speed/network filters |
| **F2 — ratings & reviews (write)** (was Open, stubbed) | ✅ **Done** | `ReviewsViewModel`; `GET/POST api/stations/{id}/reviews`; working composer (star rating + text) in `StationDetailScreen.ReviewsTabContent`; **review entry from `PaymentSuccessScreen` → Reviews tab** |
| **F3 — favorites & recents** (was Open) | ✅ **Done** | `SavedStationsScreen` + `FavoritesViewModel`; `GET/POST/DELETE api/users/me/favorites` |
| **G2 — recurring bookings** (was Open / Low) | ✅ **Done** | `RecurringBookingsScreen`; `api/users/me/booking-templates` CRUD; `BookingTemplate` model |
| **I2 — accessibility labels** (was Open) | ⚠️ **Partial** | `semantics{}`/content-descriptions added to custom composables (incl. segmented OTP). Full TalkBack funnel audit still pending |
| **I4 — Hilt/DI + refactor** (was Partial) | ⚠️ **Improved** | `MainActivity` is now `@AndroidEntryPoint` with `@Inject UserPreferencesRepository` and `by viewModels()` `ChargingViewModel` (no hand-wiring). **Still:** nav graph is a single ~713-line `MainActivity` block; most other VMs use `viewModel()`; no repository layer over `RetrofitClient` |
| **H2 — biometric app-lock** | ➖ **Implemented then removed** | Added in `09e8a52`, **removed in `4debcb7`** by product decision (`BiometricGate.kt` deleted, `androidx.biometric`/`fragment-ktx` deps dropped, `MainActivity` reverted to `ComponentActivity`). No longer a roadmap item unless re-requested |

### Wave 2b — Charging-completion & auth overhaul (2026-06-18 → 06-19)

The biggest architectural change since the last audit: **charging completion is now server-authoritative**, so a full battery finalizes and bills correctly even with the app closed, and **payment is never lost**.

| Change | Status | Where (verified) |
|---|---|---|
| **Server-authoritative completion** | ✅ Done | `service/ChargingCompletionService.finalizeSession()` — single idempotent path shared by manual stop (`ChargingSessionController.stopCharging`) **and** the simulator's auto-complete at 100%/overtime. Re-loads the entity **inside** the `@Transactional` to avoid the `LazyInitializationException` that previously rolled back the finalize and "restarted" the session (`9b4ab14`) |
| **Auto-finalize at 100% (app closed)** | ✅ Done | `ChargingSimulatorService` `@Scheduled` tick calls `finalizeSession` when full; broadcasts `completed=true` **only after** success |
| **Triple payment-recovery net** | ✅ Done | (1) FCM "Charging complete" push → `plugsy://payment/{id}`; (2) `HomeScreen.PendingPaymentBanner` (→ `GET …/outstanding`); (3) `ChargingHistoryScreen` "Pay now" on COMPLETED-unpaid rows |
| **Completion + ongoing notifications** | ✅ Done | `ChargingForegroundService`: ongoing card (`NOTIF_ID 4242`, live SoC, Stop action, resumes live screen) **swaps** to a dismissable "Charging complete — tap to pay" card (`COMPLETE_NOTIF_ID 4243`) → payment deeplink; ongoing card cleared on end; SoC rounding synced with the screen (`roundToInt`) |
| **Deeplink cold-start auth** | ✅ Done | `MainActivity.onCreate` restores access+refresh tokens into `RetrofitClient` *before* `setContent`, so a notification tap straight into the payment screen is authenticated (fixed "Failed to load session", `0e82db4`) |
| **Configurable simulation speed** | ✅ Done | `app.charging.simulation-speed` (`@Value`, default `1.0`; dev runs fast) — keeps prod realistic while a full charge tests in <5 min |
| **OTP autofill, crash-guarded** | ✅ Done | `LoginScreen` re-adds **SMS User Consent** ("Allow" prompt) with a lifecycle-bound receiver + try/catch on every step (`b361f53`); `AuthViewModel` now **suppresses Firebase silent auto-login** (`smsDispatched` flag) so the prompt is the consistent path, while still honoring **instant verification** when no SMS was sent (`bc9f142`) |
| **Misc fixes** | ✅ Done | Profile screen made scrollable (`ccb6520`); no empty 0% charging UI for a finished/loading session (`73f467e`) |

### Wave 1 — recap (closed before 2026-06-17)

| Original item | Status | Where |
|---|---|---|
| Backend SMS-OTP precondition (was #1 blocker) | ✅ Replaced with **Firebase Phone Auth** (no India DLT) | `AuthViewModel`, backend `firebase-login` |
| A1 — background charging foreground service | ✅ Done | `ChargingManager` + `ChargingForegroundService` |
| A3 — persist rotated tokens | ✅ Done | `EvApplication.setTokenPersister` |
| B1 — push (FCM) | ✅ Done | `EvMessagingService`, channels, device-token |
| H1 — in-app account deletion | ✅ Done | `DELETE api/users/me` |
| H3 — deep links / App Links | ✅ Done (hosting step pending) | manifest `autoVerify` + `plugsy://`; nav `deepLinks` |
| I1 — crash + analytics | ✅ Done | Crashlytics + `AppAnalytics` funnel |
| C2 — editable profile | ✅ Done | `PUT api/users/{id}` |
| E1 — navigation hand-off | ✅ Done | `google.navigation:` from `StationCard` |
| STOMP heartbeat `0,0` | ✅ Mitigated | OkHttp `pingInterval(20s)` |

**Net effect:** the original **High** tier (A1, A3, B1, H1, I1, D2) is fully shipped — D2 now has a real **payment receipt PDF**, leaving only **GST tax-invoice compliance** as a sub-item. What remains for a clean launch is small and concrete (see §4).

---

## Section 1: Detailed List of Current Features (Audited)

Each item verified **against source**. ✅ = present and wired; ⚠️ = present but with a caveat that affects production; ➖ = deliberately removed.

### 1.1 Authentication & Session
- ✅ **Phone login via Firebase Phone Auth** (`AuthViewModel.kt`, `LoginScreen.kt`). Flow: `PhoneAuthProvider` send/resend/verify → Firebase ID token → `POST /api/auth/firebase-login` exchanges it for the local JWT + refresh token. New users are created server-side immediately (phone verified) and then set **Name/Email** via authenticated `PUT /api/users/{id}`.
- ✅ **OTP autofill — "Allow" consent prompt, crash-guarded.** `LoginScreen` runs the **SMS User Consent API** with a lifecycle-bound `BroadcastReceiver` (re-registered per send via `DisposableEffect(resendNonce)`, guarded unregister) and `try/catch` on receiver/launch/parse — so a failure degrades to "prompt not shown once," never the crash the earlier unguarded version caused.
- ✅ **Consent prompt is now the consistent path.** `AuthViewModel` **suppresses Firebase's SMS-based silent auto-login** via an `smsDispatched` flag (set in `onCodeSent`): once an SMS is sent, a later `onVerificationCompleted` is ignored so the visible "Allow" prompt (or manual entry) completes. **Instant verification** (no SMS sent) is still honored, so the user is never stuck. Manual 6-digit entry remains the ultimate fallback.
- ✅ **Login UX** — `+91` prefix + strict 10-digit validation (`^[6-9]\d{9}$`), WhatsApp-style "verify this number?" confirm dialog, segmented 6-box OTP input (auto-advance/backspace, guarded `focusRequester`), 60s resend countdown, auto-submit at 6 digits.
- ✅ **Token lifecycle** — access + refresh persisted in DataStore (`UserPreferencesRepository`); auto-login via `SplashScreen`; **deeplink cold-starts** restore tokens into `RetrofitClient` in `MainActivity.onCreate` before any API call.
- ✅ **Silent 401 recovery** — `RetrofitClient` `Authenticator` refreshes on a separate client (no recursion), retries, capped at 3; on terminal failure emits `SessionEvents` for graceful global sign-out (A4).
- ✅ **Token-rotation persistence** — `EvApplication` hands `RetrofitClient` a persister so a refreshed token is written back to DataStore and survives restart.
- ⚠️ **Rate limiting is Firebase-side only** — `firebase-login` only verifies a token; SMS-send abuse protection relies entirely on Firebase's adaptive anti-abuse. Acceptable, but a cost-control note.
- ⚠️ **Release-signing fingerprint not yet registered** — **debug** SHA-1/SHA-256 are in Firebase (verified: debug keystore matches the console), but no release keystore exists, so Phone Auth will fail in a Play release build until the release fingerprint is added.

### 1.2 Onboarding
- ✅ **4-page slider** (`OnboardingScreen`), gated by a DataStore `should_show_onboarding` flag, shown once before login.
- ⚠️ **`clearUserData()` wipes the onboarding flag too** (`preferences.clear()`), so a logout re-shows onboarding on next launch. Minor regression on logout.

### 1.3 Map / Discovery (`HomeScreen`)
- ✅ Google Maps Compose with custom pins; viewport-debounced `getViewportWithNearby` (full data for top-N, lightweight pins for the rest); 15%-delta refetch guard.
- ✅ List/map toggle, distance sort, relative "last used" time, bottom station pager.
- ✅ **Discovery search + filters (F1)** — name/address search + **Available** + **Open now** chips, applied client-side over the loaded set; the filtered set drives both map markers and list (far pins hidden while filtering).
- ✅ **Pending-payment banner** — `PendingPaymentBanner` calls `GET …/outstanding` and surfaces a "Pay now" card for any completed-but-unpaid session.
- ✅ **Offline fallback** — `StationViewModel.emitCachedOrError` serves the last Room-cached stations on network failure.
- ✅ **Directions hand-off** — `StationCard` fires `google.navigation:q=lat,lng`.
- ⚠️ **No connector/speed/network filters** — only Available/Open-now exist; richer filtering still absent. *(Gap F1b.)*

### 1.4 Station Details (`StationDetailScreen`)
- ✅ Operating-hours parsing with live open/closed; guns grouped by dispensary; tabs **Charger / Details / Reviews** (deep-linkable to a specific tab via `?tab=`).
- ✅ Filter chips (Available / AC / DC) on the detail screen; static map preview.
- ✅ Live grid metrics (Voltage, Current, Power, Forecasted Load) via `getStationLivePower`; weighted scores via `getStationDetail`.
- ✅ **Reviews are live (F2)** — `ReviewsTabContent` (`hiltViewModel<ReviewsViewModel>()`) lists reviews and provides a star-rating + text **composer** posting to `POST api/stations/{id}/reviews`; reachable post-payment from `PaymentSuccessScreen`.

### 1.5 Slot Booking (`SlotBookingScreen` + shared `BookingViewModel`)
- ✅ Instant 20-minute reservation, connector selection (CCS2 / Type-2), vehicle class (Car / Truck), truck-fallback prompt when car slots are exhausted.
- ⚠️ **Does not use the vehicle garage** — booking still picks a generic CAR/TRUCK class; saved vehicles (make/model/**batteryKwh**/connector) are not pre-filled and don't feed remaining-time. *(Gap C1b.)*
- ⚠️ **Instant-only** — no reserve-ahead / scheduled single booking (recurring templates exist; see 1.11). *(Gap G1.)*

### 1.6 Booking Management
- ✅ Active-reservation countdown, booking detail with cancel, paginated history (`getUserBookings`, page/size).
- ✅ **Offline cache (A2)** — bookings/history JSON cached via `JsonStore` for read-path resilience.

### 1.7 Real-Time Charging (`ChargingScreen` + `ChargingViewModel` + `ChargingManager` + `StompClient`)
- ✅ STOMP-over-WebSocket telemetry: live **SoC%** (pulsing ring), power, elapsed cost, energy, remaining time, emergency stop.
- ✅ **Background survival** — socket lives in process-singleton `ChargingManager`; `ChargingForegroundService` (type `dataSync`) keeps it alive with an ongoing SoC notification + Stop action; `ChargingViewModel` mirrors `ChargingManager.telemetry`; `START_REDELIVER_INTENT` + `resume()` recover after process death. Ongoing notification deep-links back to the live screen (`?isNewSession=false`).
- ✅ **Server-authoritative completion** — when the battery hits 100% (or overtime), the **backend** `ChargingCompletionService.finalizeSession` ends/bills the session regardless of app state; the app reacts to `telemetry.completed`. No more "0% empty screen" or accidental session restart on completion.
- ✅ **Resilient socket** — exponential-backoff reconnect (2s→30s) with topic re-subscribe; JWT in CONNECT frame; OkHttp `pingInterval(20s)`.
- ⚠️ **Error-recovery is heuristic** — `ChargingScreen` still inspects error message text ("already exists", "session not found") to decide start vs. resume. Works, but brittle to backend message changes.

### 1.8 Payment & Checkout
- ✅ Invoice summary, **Razorpay** checkout (card/UPI), server-side signature verification (`payments/verify`), success screen. `MainActivity` implements `PaymentResultWithDataListener` and routes the result into the shared `ChargingViewModel`.
- ✅ **Payment recovery** — `getOutstandingSessions` + Home banner + history "Pay now" + completion push ensure an unpaid completed session is always reachable.
- ✅ **Receipt PDF (D2)** — `ReceiptService` renders a payment receipt; `PaymentSuccessScreen`/history can download + share via `ReceiptHelper`/`FileProvider`.
- ⚠️ **No stored payment instrument / wallet** — Razorpay is re-entered each session. *(Gap D1.)*
- ⚠️ **Receipt is not a GST tax invoice** — no GSTIN/tax-breakup/HSN (company not GST-registered); layout pre-structured for it. *(Gap D2b.)*

### 1.9 Profile (`ProfileScreen` + `ProfileViewModel`)
- ✅ **Scrollable** (fixed), editable name/email (inline → `PUT /api/users/{id}`), shows mobile/role/member-since/last-updated, Logout, and entries to **Settings / Saved / Vehicles / Recurring**, plus **Delete Account** (confirm → `DELETE /api/users/me`).
- ⚠️ **No profile photo**; no data-export path. *(Gaps C2-minor, H1b.)*

### 1.10 Settings (`SettingsScreen`)
- ✅ **Notifications** — master switch + per-category (Charging / Reminders / Payments), **enforced** at display time via `NotificationPrefs` (B2).
- ✅ **Appearance** — real **System / Light / Dark** theme switch persisted to DataStore and applied app-wide (C3b).
- ✅ About (app/version).
- ⚠️ **Help & Support is a static email line** — no FAQ or contact intent; no language/units. *(Gap C3c.)*

### 1.11 Vehicles, Favorites, Trip, Recurring (new surfaces)
- ✅ **Vehicle garage (C1)** — `VehiclesScreen` add/list/delete (make/model/batteryKwh/connector). *Not yet consumed by booking (C1b).*
- ✅ **Favorites & recents (F3)** — `SavedStationsScreen` + favorites API.
- ✅ **Trip / route planning (E2)** — `RoutePlanScreen` + `DirectionsApi`; bottom-nav **Trip** tab.
- ✅ **Recurring bookings (G2)** — `RecurringBookingsScreen` + booking-template CRUD.

### 1.12 Architecture baseline
- ✅ MVVM + `StateFlow`; Navigation-Compose single sealed `Screen` graph; Material 3 + custom "Clay" theme (now light **and** dark); Coil; **Hilt**; **Room**; **Firebase** (Auth/Messaging/Analytics/Crashlytics); **WorkManager**.
- ⚠️ **Hilt partially adopted** — `MainActivity` injects prefs + uses `by viewModels()` for `ChargingViewModel`, but the nav graph is still a ~713-line `MainActivity` monolith, most VMs use `viewModel()`, and no repository layer abstracts `RetrofitClient`. *(Gap I4.)*
- ⚠️ **Room/cache** — `StationEntity` + a generic `JsonStore` cache (bookings/history); reasonable, but not a full offline model. *(Gap A2-minor.)*
- ⚠️ **Onboarding flag cleared on logout** (see 1.2).

---

## Section 2: Categorized Gaps & Missing Features

Status tags reflect the 2026-06-19 re-audit: **[DONE]**, **[PARTIAL]**, **[OPEN]**, **[REMOVED]**.

### A. Reliability, Background & Offline
- **A1 — Background charging foreground service.** **[DONE]**
- **A2 — Offline cache + WorkManager.** **[DONE]** Stations + bookings/history JSON cache; WorkManager device-token retry. (Could extend retry to payment-verify.)
- **A3 — Persist rotated tokens.** **[DONE]**
- **A4 — Graceful global sign-out.** **[DONE]** `SessionEvents` → `MainActivity` collector.
- **A5 — Server-authoritative completion.** **[DONE]** `ChargingCompletionService` (new since last audit).

### B. Notifications & Alerts
- **B1 — Push (FCM).** **[DONE]**
- **B2 — Notification preferences + runtime permission.** **[DONE]** Enforced master + per-category; `POST_NOTIFICATIONS` requested at first charging.
- **B3 — Dedupe completion notifications.** **[OPEN, minor]** A completed charge can fire both the local "tap to pay" card and the backend FCM completion push. Optional: suppress one.

### C. Profile & Vehicle Management
- **C1 — Vehicle garage.** **[DONE]** CRUD shipped.
- **C1b — Wire garage into booking + remaining-time.** **[OPEN]** Pre-fill connector from the saved vehicle; use `batteryKwh` for accurate remaining-time; prerequisite for trip range (E2 polish).
- **C2 — Editable profile.** **[DONE]** (photo still absent — minor).
- **C3 — Settings hub.** **[DONE]** (theme + notif done).
- **C3c — Help/FAQ + language/units.** **[OPEN]** Help is a static email; no i18n/units.

### D. Payment, Wallet & Loyalty
- **D1 — Saved payment / prepaid wallet.** **[OPEN]**
- **D2 — Receipts.** **[DONE]** Payment-receipt PDF + download/share.
- **D2b — GST tax invoice.** **[OPEN]** Add GSTIN, tax breakup, HSN/SAC when GST-registered (layout pre-structured). Needed for the truck/fleet expense flow.
- **D3 — Promo / referral / loyalty.** **[OPEN]**
- **D4 — Auto-charge on session end (post-paid).** **[OPEN]** Depends on D1.

### E. Navigation & Route Planning
- **E1 — Turn-by-turn hand-off.** **[DONE]** (could also surface on `StationDetailScreen`).
- **E2 — Corridor / trip planning.** **[DONE]** Range-awareness still improves once C1b lands.

### F. Discovery & Social Trust
- **F1 — Map/list search + basic filters.** **[DONE]** (search, Available, Open-now).
- **F1b — Connector/speed/network filters.** **[OPEN]**
- **F2 — Ratings, reviews, photos.** **[DONE]** (text + rating; photos still absent).
- **F3 — Favorites & recents.** **[DONE]**

### G. Booking Depth
- **G1 — Reserve-ahead / scheduled single bookings.** **[OPEN]**
- **G2 — Recurring bookings.** **[DONE]**

### H. Account, Compliance & Security
- **H1 — Account deletion.** **[DONE]** (in-app).
- **H1b — Public web deletion URL + data export.** **[OPEN]** Play Console requires a web deletion URL even with in-app deletion; DPDP export right.
- **H2 — Biometric app-lock.** **[REMOVED]** Implemented then removed by product decision; re-open only if desired.
- **H3 — Deep links / App Links.** **[DONE]** wired; **App Links `https` auto-verification still needs `https://plugsy.in/.well-known/assetlinks.json` hosted** (H3b).

### I. Quality & Observability
- **I1 — Crash reporting + analytics.** **[DONE]**
- **I2 — Accessibility pass.** **[PARTIAL]** Labels added; full TalkBack funnel audit pending.
- **I3 — Localization (i18n).** **[OPEN]** Strings still hard-coded; Hindi first for trucking.
- **I4 — Architecture hardening.** **[PARTIAL]** DI improved; still: split the `MainActivity` nav graph, extract a repository layer, inject remaining VMs.

---

## Section 3: Prioritized Implementation Roadmap (revised 2026-06-19)

Priority key: **High** = required for a credible commercial launch. **Medium** = retention/expectation; fast-follow. **Low** = differentiating/polish.

| Feature | Status | Priority | Business / User Impact | Technical Notes |
|---|---|---|---|---|
| A1/A2/A3/A4/A5, B1/B2, C1, C2/C3, D2, E1/E2, F1/F2/F3, G2, H1, H3, I1 | ✅ Done | — | Core reliability + most retention surface now shipped. | See §1–2. |
| **D2b — GST tax invoice** | **[OPEN]** | **High** | The remaining feature gate for truck/fleet expensing. | Add GSTIN/tax-breakup/HSN to `ReceiptService` once registered. |
| **H1b — Public deletion URL + data export** | **[OPEN]** | **High** | Play Console requires a web deletion URL; DPDP export right. | Static web page hitting the delete flow; `GET /api/users/me/export`. |
| **H3b — Host `assetlinks.json`** | **[OPEN]** | **High (small)** | Without it, `https://plugsy.in` App Links don't auto-verify. | Publish `/.well-known/assetlinks.json` with the signing SHA-256. |
| **Release-keystore SHA in Firebase** | **[OPEN]** | **High (small)** | Phone Auth **breaks in the signed release** until added. | Create release keystore → add SHA-1/256 → re-download `google-services.json`. |
| **B3 — Dedupe completion notifications** | **[OPEN]** | **Low** | Two completion alerts (local card + FCM) feel redundant. | Suppress backend push when local card shown, or vice-versa. |
| C1b — Wire vehicle garage into booking | [OPEN] | **Medium** | Accurate remaining-time + auto-connector; payoff for the garage already built. | Pre-fill `SlotBookingScreen` from saved vehicle; use `batteryKwh`. |
| F1b — Connector/speed/network filters | [OPEN] | **Medium** | "DC, available now, on my network" narrowing. | Extend the existing Home filter state. |
| D1 — Saved payment / wallet | [OPEN] | **Medium** | Removes per-session checkout friction (dominant India pattern). | Razorpay tokenization or backend wallet. |
| I4 — Finish Hilt + repo refactor + split nav graph | ⚠️ Partial | **Medium (enabler)** | ~713-line `MainActivity` throttles velocity. | Extract nav graph; repository layer over `RetrofitClient`; inject remaining VMs. |
| C3c — Help/FAQ + language/units | [OPEN] | **Medium** | Settings still has a static support line. | FAQ screen + contact intents; ties to I3. |
| I2 — Accessibility audit | ⚠️ Partial | **Medium** | Labels exist; needs TalkBack pass on booking→charging→payment. | Scanner + remaining `semantics{}`. |
| G1 — Reserve-ahead / scheduled booking | [OPEN] | **Low** | Removes "only book when standing there." | `BookingRequest` + start time; window availability. |
| D3 — Promo / referral / loyalty | [OPEN] | **Low** | Growth lever once paid funnel is solid. | Promo at intent creation; referral via `ACTION_SEND`. |
| F2-photos / F1b polish | [OPEN] | **Low** | Richer trust signals. | Review photo upload; richer filters. |
| I3 — Localization (Hindi) | [OPEN] | **Low** | Inclusivity for trucking; cheap now. | Externalize strings; Hindi first. |
| iOS / cross-platform | [OPEN] | Won't (this release) | Validate the funnel on Android first. | Later: KMP / mobile-web. |

---

## Section 4: Priority Rationale (revised)

**The product is now well past "make it shippable."** Since the 2026-06-16/17 audits, not only is the reliability/store-gate layer built (Firebase Phone Auth, foreground charging service, FCM, in-app deletion, Crashlytics/Analytics), but **most of the Medium/Low retention roadmap also shipped** — vehicles, favorites, trip planning, recurring bookings, reviews, theme, notification prefs, offline cache, graceful sign-out, and a payment-receipt PDF. On top of that, **charging completion is now server-authoritative**, closing the most damaging real-world failure mode (a full battery that never finalized/billed while the app was closed) and wrapping it in a triple payment-recovery net.

**What actually still blocks a clean store launch is now narrow and mostly paperwork:**
1. **D2b — GST tax invoice.** The receipt PDF exists; making it a compliant tax invoice is the last *feature* gate for the truck/fleet segment, and is blocked on GST registration, not code.
2. **H1b / H3b / release-SHA — the gates.** A public web deletion URL, the hosted `assetlinks.json`, and the **release-keystore fingerprint in Firebase** (without which Phone Auth breaks the instant you ship a signed release). Low-effort, each independently launch-blocking → **High**.

**The Medium tier is now "harvest what's half-built":** wire the vehicle garage into booking/remaining-time (C1b — the data already exists), add the richer discovery filters (F1b), saved payment (D1), and finish the architecture refactor (I4) so the ~713-line `MainActivity` doesn't throttle the next wave.

**The Low tier (reserve-ahead, loyalty, review photos, i18n, notification dedupe) stays last** — polish that multiplies a working core but can't substitute for the four gates above.

**Suggested mobile sequencing (revised):**

| Phase | Items | Theme |
|---|---|---|
| **Phase 1 — Close the launch gates** | D2b (GST), H1b (web URL + export), H3b (assetlinks.json), release-keystore SHA | Compliance + store/release paperwork |
| **Phase 2 — Harvest the half-built** | C1b, F1b, D1, I4 (nav-graph split + repo layer), C3c, I2 audit | Convert shipped surfaces into retention + maintainability |
| **Phase 3 — Differentiate / polish** | G1, D3, F2-photos, I3, B3 | Scheduling, growth, inclusivity, notification cleanup |

**One-line bottom line:** the launch gap is now **GST invoicing plus three small store/release gates**; nearly the entire feature roadmap has shipped, completion is server-authoritative, and the real engineering frontier has shifted to **wiring up the surfaces already built (vehicles, filters, payment) and splitting the `MainActivity` monolith.**
