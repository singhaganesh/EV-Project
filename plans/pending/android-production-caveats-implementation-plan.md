# Android Driver App — Production Caveats: Triage & Implementation Plan ("Plugsy")

**Date:** 2026-06-16
**Source:** the ⚠️ "present but with a caveat that affects production" items from `plans/pending/android-app-product-audit-and-roadmap.md`.
**Goal:** decide which caveats to fix before launch, which to defer, and give a step-by-step build plan for the ones we fix.
**Scope:** Android driver app (`com.ganesh.ev`). Backend/web counterparts are flagged as cross-team dependencies, not built here.

---

## Part A — All 13 Caveats: What & Why (short)

| ID | Caveat (present, but flawed) | Why it matters in production | Verdict |
|----|------------------------------|------------------------------|---------|
| **CV-1** | **Rotated token not persisted.** `RetrofitClient` refreshes the access token in memory only; never written back to DataStore. | After every app restart the app loads a stale token → an extra failing call, and a **silent logout** if the backend rotates refresh tokens. Real bug, tiny fix. | ✅ **Implement now** |
| **CV-5** | **No background survival for charging.** Socket + session live in `ChargingViewModel`/Activity; no foreground service, no `FOREGROUND_SERVICE` permission. | The core promise — "watch your charge" — dies the moment the screen sleeps or the app is backgrounded. On process death the live session is lost. Highest experiential + billing-trust risk. | ✅ **Implement now** |
| **CV-6** | **STOMP heartbeat `0,0`** and bare `OkHttpClient()` (no WS ping). | Half-open connections aren't detected promptly; telemetry can silently freeze without triggering the existing reconnect. One-line fix, big reliability gain. | ✅ **Implement now** |
| **CV-8** | **Read-only profile** — no edit, no settings, **no account deletion**. | Account deletion is a **Google Play listing requirement** (and DPDP Act right) → store blocker. Editable profile + settings are expected basics. | ✅ **Implement now** (deletion = must; edit/settings = should) |
| **CV-11** | **No FCM push** (declared `POST_NOTIFICATIONS`, zero Firebase). | A driver who walks away never learns charging finished / hold expired / was force-stopped & billed → churn + billing disputes. | ✅ **Implement now** |
| **CV-12** | **No deep links / App Links** (only `LAUNCHER` intent). | Prerequisite so notification taps (CV-11), shared station links, and emailed receipts open the right screen. | ✅ **Implement now** |
| **CV-13** | **No crash reporting / analytics.** | You cannot launch blind — the first crash and the first funnel drop-off must be observable. Cheap; rides on the same Firebase setup as CV-11. | ✅ **Implement now** |
| **CV-9** | **No dependency injection (Hilt).** `MainActivity` (~550 lines) hand-wires ViewModels/repos. | Not user-facing, but throttles delivery of everything else and makes the CV-5 refactor messy. Best done *with* CV-5/CV-10. | 🟡 **Foundation (recommended)** |
| **CV-10** | **DataStore-only; no Room/WorkManager → no offline cache.** | The moment of need (poor signal at the charger) is exactly when a network-only app shows a blank screen. | 🟡 **Foundation (recommended)** |
| **CV-2** | **No discovery filters/search** (filters only on detail screen). | Real UX gap, but it's a **net-new feature** (roadmap F1), not hardening of existing code. | ⏸️ **Defer to roadmap** |
| **CV-3** | **`Station.rating` display-only** (no reviews write path). | Important trust feature, but fixing it = **building a whole reviews system** (roadmap F2). | ⏸️ **Defer to roadmap** |
| **CV-4** | **Instant-only booking** (no reserve-ahead). | Convenience feature (roadmap G1), not a production-hardening fix. | ⏸️ **Defer to roadmap** |
| **CV-7** | **No saved payment / wallet / receipt artifact.** | Wallet/saved-card = feature (roadmap D1); receipts/invoices depend on a **backend PDF service** (D2). Not Android-only hardening. | ⏸️ **Defer to roadmap** |

---

## Part B — Triage Rationale ("which to implement, which to ignore")

**Not all 13 are equal, and lumping them together would dilute the plan.** They split cleanly into three groups:

1. **Implement now (7): CV-1, CV-5, CV-6, CV-8, CV-11, CV-12, CV-13.**
   These are either (a) genuine *fix-what-exists* hardening (CV-1, CV-5, CV-6) or (b) *gates to ship* — store/legal (CV-8 deletion), the walk-away alert loop (CV-11 + CV-12), and operational visibility (CV-13). None can be substituted by a feature; the app is not credibly launchable without them.

2. **Foundation, recommended (2): CV-9 (Hilt), CV-10 (Room/offline).**
   Not user-facing and technically optional, but they make CV-5 clean and give offline resilience. Do them *alongside* CV-5 (which already restructures data ownership) rather than retrofitting later. If you need maximum speed, the "Implement now" set works on the current manual wiring — but CV-9/CV-10 are the difference between a hardened app and one that just patches symptoms.

3. **Defer — not "ignore," but tracked elsewhere (4): CV-2, CV-3, CV-4, CV-7.**
   Each is a **net-new feature already prioritized in the roadmap** (F1, F2, G1, D1/D2), not a hardening of existing code. They're important for stickiness/growth, but building them in this pass would balloon scope and delay launch. They keep their roadmap home; this plan deliberately leaves them out.

> If you want everything in one go, the step plans for CV-2/CV-3/CV-4/CV-7 can be appended — but the recommendation is to ship the hardening set first.

---

## Part C — Step-by-Step Implementation Plan (the "Implement now" + Foundation set)

> Workflow per the repo convention: after each step, build (`cd android` → `./gradlew :app:assembleDebug`; Windows: `gradlew.bat`), then commit with a scoped message. Keep steps independently shippable.

### Phase 1 — Quick correctness & reliability wins (low effort, high ROI)

#### Step 1 — CV-1: Persist rotated tokens
- **Files:** `data/network/RetrofitClient.kt`, `MainActivity.kt` (or a new `EvApplication`), verify `data/model/AuthResponse`/`Models.kt` exposes `refreshToken`.
- **Changes:**
  1. In `RetrofitClient` add `@Volatile private var tokenPersister: ((access: String, refresh: String?) -> Unit)? = null` and `fun setTokenPersister(p: (String, String?) -> Unit)`.
  2. In the `Authenticator` success branch, after `authToken = newData.token`, also capture `newData.refreshToken` (if the backend rotates it) into the in-memory `refreshToken`, then call `tokenPersister?.invoke(newData.token, newData.refreshToken)`.
  3. On app start, register the persister with a process-scoped IO scope: `RetrofitClient.setTokenPersister { access, refresh -> appScope.launch { repo.saveAuthToken(access); refresh?.let { repo.saveRefreshToken(it) } } }`.
- **Verify:** log in → force a 401 / wait for access-token expiry → confirm DataStore `user_token` updates; kill & relaunch → no forced logout.

#### Step 2 — CV-6: WebSocket dead-connection detection
- **Files:** `data/network/StompClient.kt`.
- **Changes:**
  1. Replace `private val client = OkHttpClient()` with `OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()` so OkHttp sends WS pings; a dead peer triggers `onFailure` → the existing `scheduleReconnect()`.
  2. *(Optional, belt-and-suspenders)* negotiate STOMP `heart-beat:10000,10000` in the CONNECT frame and send a `"\n"` keepalive on the existing `scheduler`; treat prolonged server silence as a reconnect trigger.
- **Verify:** start a session → toggle airplane mode → confirm reconnect within ~ping interval and telemetry resumes.

### Phase 2 — Store/legal gate (small, unblocks listing)

#### Step 3 — CV-8a: Account deletion
- **Files:** `data/network/ApiService.kt`, `ui/screens/ProfileScreen.kt`. **Backend dep:** `DELETE /api/users/me` (soft-delete + PII anonymization, retain financial rows for tax) + a public deletion-instructions URL for the Play Console field.
- **Changes:**
  1. Add `@DELETE("api/users/me") suspend fun deleteAccount(): Response<ApiResponse<Void>>`.
  2. ProfileScreen: a destructive **"Delete Account"** action → confirmation dialog (explicit, e.g. type "DELETE") → on success: `clearUserData()` + `RetrofitClient.clearAuthTokens()` → navigate to `login` (mirror the existing logout path).
- **Verify:** delete in app → server anonymizes the account, app returns to login, the deleted number can't log back in.

### Phase 3 — Observability + notifications (shared Firebase setup)

#### Step 4 — CV-13: Crash reporting + analytics (also bootstraps Firebase)
- **Files:** root `build.gradle.kts` (google-services + crashlytics plugin classpaths), `app/build.gradle.kts` (Firebase BOM + `firebase-crashlytics`, `firebase-analytics`), `app/google-services.json` (from Firebase console; gitignore it), new `util/Analytics.kt` wrapper.
- **Changes:**
  1. Apply `com.google.gms.google-services` and `com.google.firebase.crashlytics` plugins; add the BOM + Crashlytics + Analytics deps.
  2. Behind an `Analytics` interface, log the funnel: `station_view`, `booking_created`, `charging_started`, `payment_success`, `charging_completed`.
- **Verify:** trigger a test crash → appears in Crashlytics; events show in Analytics DebugView.

#### Step 5 — CV-12: Deep links / App Links (do before CV-11 taps)
- **Files:** `MainActivity.kt` (add `deepLinks` to the relevant `composable`s), `AndroidManifest.xml` (App Links intent-filter with `autoVerify="true"`). **Web dep:** host `/.well-known/assetlinks.json` on `plugsy.in`.
- **Changes:**
  1. Add `navDeepLink { uriPattern = "https://plugsy.in/charging/{bookingId}" }` (and `/bookings/{userId}`, `/station/{stationId}`) onto the matching routes in the existing `Screen` graph.
  2. Add the `https` `autoVerify` intent-filter for the host.
- **Verify:** `adb shell am start -a android.intent.action.VIEW -d "https://plugsy.in/charging/123"` opens the charging screen.

#### Step 6 — CV-11: FCM push notifications
- **Files:** `app/build.gradle.kts` (`firebase-messaging`), `AndroidManifest.xml` (service decl), new `data/notifications/EvMessagingService.kt`, `ApiService.kt` (device-token endpoint), notification-channel setup, runtime permission request. **Backend dep:** `POST /api/users/device-token` + send pushes on the existing triggers (force-stop, booking-expiry scheduler, session completion).
- **Changes:**
  1. `EvMessagingService : FirebaseMessagingService` — `onNewToken` → register via `POST /api/users/device-token`; `onMessageReceived` → build a notification with a deep-link `PendingIntent` (CV-12).
  2. Create channels: **charging** (ongoing/high), **reminders** (hold-expiry T-5/T-1), **transactional** (payment).
  3. Register the token on login and on refresh; request `POST_NOTIFICATIONS` at runtime (Android 13+) at first charging.
- **Verify:** send a test push from Firebase console → notification shows; tap routes via deep link.

### Phase 4 — Core reliability refactor (the big one)

#### Step 7 — CV-5: Foreground charging service
- **Files:** new `service/ChargingForegroundService.kt`, `AndroidManifest.xml` (permissions + service), refactor `ui/viewmodel/ChargingViewModel.kt` + `ui/screens/ChargingScreen.kt`, new shared holder `data/charging/ChargingSessionState.kt`.
- **Design:**
  1. Add permissions `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` (API 34) and declare the service with `foregroundServiceType="dataSync"`.
  2. **Move `StompClient` ownership + telemetry parsing out of `ChargingViewModel` into the service.** Expose telemetry/status as `StateFlow`s on a process-singleton `ChargingSessionState` (or a bound-service `Binder`).
  3. Start the service when charging begins (`startForegroundService` → `startForeground` with an **ongoing notification** showing live SoC + a **Stop** action). Stop the service on session stop/complete.
  4. `ChargingViewModel` becomes a thin reader of `ChargingSessionState`; `ChargingScreen` collects it. On cold start / process restart the service re-fetches `getSessionByBooking` and resumes telemetry.
  5. The notification **Stop** action invokes `stopCharging`.
- **Verify:** start charging → lock screen / background app several minutes → reopen: telemetry still live, notification updating; Stop-from-notification ends the session.

### Phase 5 — Profile completion (should)

#### Step 8 — CV-8b: Editable profile + Settings hub
- **Files:** `ApiService.kt` (`PUT api/users/{id}`), `ProfileScreen.kt` (stateful form or new `EditProfileScreen`), new `SettingsScreen` + route in the `Screen` sealed class & NavHost, notification-pref toggles (DataStore).
- **Changes:** editable name/email/photo; Settings: notification toggles (feed CV-11 channels), theme, language placeholder, app version, links to Help & the deletion path (CV-8a).
- **Verify:** edits persist server-side; settings toggles persist across restart.

### Foundation (recommended; run alongside Phase 4)

#### Step F1 — CV-9: Hilt DI
- **Files:** `app/build.gradle.kts` (hilt plugin + deps), new `@HiltAndroidApp class EvApplication`, `@AndroidEntryPoint MainActivity`, `@HiltViewModel` on ViewModels with `@Inject` constructors, a `di/AppModule` providing `ApiService`, repositories, DataStore.
- **Outcome:** removes the manual `ChargingViewModel()` and the passed-down repository; makes the CV-5 service injectable.
- **Verify:** app builds and runs with VMs/repos injected; no behavior change.

#### Step F2 — CV-10: Room offline cache
- **Files:** new `data/local/` (`AppDatabase`, `StationEntity`/`BookingEntity`/`SessionEntity`, DAOs), repository layer returning cache-then-network `Flow`s.
- **Changes:** repositories emit cached data immediately, then refresh from `ApiService`; `HomeScreen`/`MyBookings`/history read the cache first.
- **Verify:** cold-launch in airplane mode shows last-known stations/bookings instead of a blank screen.

---

## Sequencing, Dependencies & Effort

| Order | Step | Rough effort | Depends on |
|-------|------|--------------|------------|
| 1 | CV-1 token persist | ~0.5 day | — |
| 2 | CV-6 WS ping | ~0.5 day | — |
| 3 | CV-8a account deletion | ~1 day (+ backend) | backend `DELETE /users/me` |
| 4 | CV-13 crash/analytics | ~1 day | Firebase project + `google-services.json` |
| 5 | CV-12 deep links | ~1 day | `assetlinks.json` on web host |
| 6 | CV-11 FCM push | ~3–4 days (+ backend) | CV-13 (Firebase), CV-12 (taps), backend device-token + triggers |
| 7 | CV-5 foreground service | ~4–6 days | best with F1/F2 |
| 8 | CV-8b profile/settings | ~2–3 days | CV-11 (notif prefs) |
| F1/F2 | Hilt + Room | ~3–5 days | run alongside step 7 |

**Cross-team (backend/web) dependencies to open in parallel:** SMS-OTP delivery (the upstream launch precondition), `DELETE /api/users/me`, `POST /api/users/device-token` + push triggers, public account-deletion URL, `assetlinks.json` hosting, and (for deferred CV-7) invoice-PDF generation.

## Definition of Done (launch gate for this plan)
- [ ] CV-1: token survives app restart; no spurious logout.
- [ ] CV-6: telemetry recovers automatically after a dropped/dead connection.
- [ ] CV-8a: in-app account deletion works end-to-end; public URL live for Play Console.
- [ ] CV-13: crashes + funnel events visible in dashboards.
- [ ] CV-12 + CV-11: a charge-complete push arrives while backgrounded and its tap opens the right screen.
- [ ] CV-5: charging telemetry + notification survive backgrounding, screen-off, and process death.
- [ ] (Recommended) CV-9/CV-10 landed so the above sits on a maintainable, offline-capable base.
