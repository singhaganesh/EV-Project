# Plugsy Android — Phased Implementation Plan

**Created:** 2026-06-17
**Owner:** Ganesh
**Source:** Derived from `android-app-product-audit-and-roadmap.md` (re-audited 2026-06-17).
**Scope:** Consumer/driver Android app (`com.ganesh.ev`) + the backend endpoints those features require.

This is the **action plan** for the remaining roadmap. Each item becomes its own commit (build → verify → commit → push), matching the established workflow. Status legend: ⬜ not started · 🔄 in progress · ✅ done · ⏸ deferred (waiting on you).

---

## Key decisions captured (2026-06-17)

- **D2 invoices →** build a **basic payment receipt (no GST)** now, structured so adding GST fields later is a small change, not a rewrite. **Reason:** company is not registered yet, so a legal GST invoice (needs GSTIN) is not possible — and being pre-registration means pre-commercial-launch, so the GST gate isn't urgent.
- **Release keystore + SHA →** ⏸ deferred — *Ganesh will do later.*
- **`assetlinks.json` hosting →** ⏸ deferred — *Ganesh will do later.*

---

## Your tasks (NOT code — I can't do these; do them when ready)

These gate a real public launch but don't block development. Listed here so they aren't forgotten:

- ⏸ **Register the company + get GSTIN** → unblocks upgrading the basic receipt to a full **GST invoice**, Razorpay **live** KYC, and B2B/fleet billing.
- ⏸ **Create a release keystore + register its SHA-1/SHA-256 in Firebase**, then re-download `google-services.json` → otherwise **Phone Auth breaks in the signed release build**.
- ⏸ **Host `https://plugsy.in/.well-known/assetlinks.json`** (with the release cert SHA-256) → makes `https://plugsy.in/...` App Links open *in the app* (notification taps, shared links). Until then only the `plugsy://` scheme resolves reliably.
- ⏸ **Play Developer account + public account-deletion URL** for the store listing (Google requires a web deletion page even with in-app deletion).
- ⏸ **Razorpay live-mode KYC** (needs business details) before collecting real payments.

---

## Phase 1 — Launch groundwork (what's buildable now)

### 1.1 ⬜ Basic payment receipt (upgradeable to GST) — *D2 (partial)*
- **Backend:** new endpoint to generate a **receipt PDF** for a paid session — session id, station, date/time, energy (kWh), unit price, total amount, payment id/method. **No tax fields yet.** Built so GSTIN / tax-breakup / HSN-SAC can be added later behind the same endpoint.
- **App:** "Download / Share receipt" action on `PaymentSuccessScreen` and on each paid row in `ChargingHistoryScreen`, opening/sharing the PDF via `FileProvider`/Intent.
- **User sees:** a downloadable/shareable receipt after every paid session.
- **Upgrade path (later, after GSTIN):** add company GSTIN, tax breakup, HSN/SAC, "Tax Invoice" title → becomes a compliant GST invoice with no app rewrite.

> Rest of Phase 1 (release keystore/SHA, assetlinks.json, Play URL, GST upgrade) = **your tasks above**, deferred.

---

## Phase 2 — Finish the partials + retention quick wins (no company needed)

Pure app/feature work. Suggested order top-to-bottom (quick wins first, refactor last).

### 2.1 ⬜ A4 — Graceful global sign-out *(app)*
When the refresh token finally fails, auto-route to `login` and clear state instead of dead-ending on failing screens. Emit an auth event from the network layer; collect it in the app shell.

### 2.2 ⬜ B2 — Notifications honor settings *(app)*
Make the Settings notifications toggle **actually suppress** pushes when off, and split into **per-category** toggles (Charging / Reminders / Payments). Enforce in `Notifications.show`.

### 2.3 ⬜ C3b — Real theme switch *(app)*
Light / Dark / System selector in Settings, persisted in DataStore and applied app-wide (currently "follows system" only).

### 2.4 ⬜ H2 — Biometric app-lock *(app)*
Optional fingerprint/face unlock on app open and before payment, using `androidx.biometric`; toggle in Settings.

### 2.5 ⬜ F1 — Discovery search + filters *(app)*
Search box (name/address) + filter chips (DC/AC, available-now, connector) on the Home map/list. Add a `FilterState` flow to `StationViewModel`.

### 2.6 ⬜ F3 — Favorites & recents *(app + backend)*
Heart to save stations (`user_favorite_station`); "Recent" list derived from history.

### 2.7 ⬜ A2 — Offline booking/history + WorkManager *(app)*
Extend Room beyond stations to **active booking + history** (visible offline at the charger); add **WorkManager** to retry device-token registration and payment verification on network loss.

### 2.8 ⬜ C1 — Vehicle garage *(app + backend)*
Save vehicles (make/model/**battery kWh**/preferred connector); auto-fill connector at booking and improve "time remaining." Prerequisite for route planning (E2).

### 2.9 ⬜ I4 — Finish Hilt + repository refactor *(app, no visible change)*
Inject `ChargingViewModel` + `UserPreferencesRepository` (stop hand-wiring in `MainActivity`), add a repository layer over `RetrofitClient`, split the ~590-line nav graph. **Do last in Phase 2** since it touches a lot.

---

## Phase 3 — Differentiators (after the core is solid)

### 3.1 ⬜ F2 — Ratings & reviews *(app + backend)*
After a `COMPLETED` session, let drivers rate/review a station; show them in the (currently empty) Reviews tab; recompute `Station.rating`.

### 3.2 ⬜ D1 — Wallet / saved payment *(app + backend — DECISION NEEDED)*
Prepaid wallet + auto-reload **or** saved-card tokenization → one-tap checkout. **Needs your choice** of model before building.

### 3.3 ⬜ E2 — Route / trip planning *(app + backend)*
Destination → charging stops along the route. **Depends on C1** (vehicle range).

### 3.4 ⬜ G1 — Reserve-ahead booking *(app + backend — DECISION NEEDED)*
Book a slot for a future time (rules: how far ahead, hold policy).

### 3.5 ⬜ G2 — Recurring bookings *(app + backend — DECISION NEEDED)*
Repeating reservations (commuter/fleet templates).

### 3.6 ⬜ D3 — Promo codes & referrals *(app + backend — DECISION NEEDED)*
Promo field at checkout + referral share.

### 3.7 ⬜ I3 — Localization (Hindi) *(app — I scaffold, you translate)*
Externalize strings to resources; you provide Hindi translations.

### 3.8 ⬜ I2 — Accessibility pass *(app)*
TalkBack on the booking→charging→payment funnel; `semantics{}` on Clay/telemetry/OTP composables.

---

## Decisions I'll need from you before certain items

| Item | Question |
|---|---|
| D1 wallet | Prepaid wallet ledger **or** saved-card tokenization? |
| G1 reserve-ahead | How far ahead can users book? Hold/no-show policy? |
| G2 recurring | Recurrence model (daily/weekly/custom)? |
| D3 promo | Discount rules (flat/%/first-ride)? referral reward? |
| D2 → GST upgrade | Provide GSTIN, legal name, address, invoice-number format once registered. |

---

## Execution rules
- One feature = one commit = one push (build + verify before committing).
- App changes verified with `./gradlew :app:assembleDebug`; backend with `./mvnw compile`.
- Scope each change tightly; avoid bundling unrelated items.
