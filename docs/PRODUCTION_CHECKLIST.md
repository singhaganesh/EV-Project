# Production Readiness Checklist

Code changes for the 32 audit issues are merged. The items below are the
**manual / external actions** that cannot be done from code. Do these before (or
as part of) the production cutover.

## 0. Rotate exposed secrets (do first)
- [ ] **GitHub PAT** that was pasted into chat — revoke it (Settings → Developer
      settings → Personal access tokens). Pushes use Git Credential Manager; the
      token is not needed.
- [ ] **Google Maps API key** (`AIza…` in `android/local.properties`) was visible
      during this work — rotate and/or restrict it (see #28 below).

## 1. Backend environment variables (issues #4, #5)
Set these in the prod environment (no defaults are baked in anymore):
- [ ] `JWT_SECRET` — generate a fresh 256-bit+ value, e.g.
      `openssl rand -base64 48`. Rotating invalidates existing tokens.
- [ ] `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — **rotate the Supabase password**
      first, then set these. Consider appending
      `&options=-c%20lock_timeout%3D5000` to `DB_URL` (transaction-mode pooler).
- [ ] `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` — **rotate the Razorpay keys** in
      the Razorpay dashboard, then set these.
- [ ] `SPRING_PROFILES_ACTIVE=prod` — activates the prod profile (validate +
      Flyway + OTP exposure off + quiet logging). Without this the app defaults
      to the `dev` profile.
- [ ] `SEED_ADMIN_PASSWORD` — only set in non-prod if you want the dev seeder to
      create `admin@ev.com`. In prod, create the first admin deliberately.

## 2. Database migration (issues #14, #21, #22, #23)
Flyway ships **disabled** so nothing auto-runs against the live DB.
- [ ] Review `backend/src/main/resources/db/migration/V2__*.sql` and `V3__*.sql`.
- [ ] Clean any rows that would violate the new constraints first:
      negative `amount`/`total_cost`, duplicate `transaction_id`, more than one
      active booking per slot, out-of-range `soc_percentage`, invalid enum strings.
- [ ] Take a DB backup/snapshot.
- [ ] Set `spring.flyway.enabled=true` (the prod profile already does this) — it
      baselines the current schema at V1 and applies V2/V3.
- [ ] Confirm the app starts cleanly with `ddl-auto=validate` (prod profile).

## 3. OTP delivery (issue #3)
- [ ] Integrate an SMS gateway (e.g. MSG91/Twilio) to actually deliver the OTP.
      Until then, dev reads it from the `send-otp` response (dev profile only);
      prod returns no OTP in the response.

## 4. Google Maps key restriction (issue #28)
- [ ] In Google Cloud Console, restrict the Maps key to the Android app:
      Application restriction = Android apps, with package `com.ganesh.ev` and the
      release signing SHA-1. Restrict the API to Maps SDK for Android.

## 5. Android release build (issues #24, #25)
- [ ] Set `RELEASE_BASE_URL=https://api.plugsy.in/` (or confirm the default) and
      build a **release** variant — it disables cleartext and uses HTTPS.
- [ ] Verify on a device: login, live charging telemetry over WebSocket
      (CONNECT now requires the JWT), reconnect after network drop, and that the
      socket is released when leaving the charging screen.

## 6. Smoke tests after deploy
- [ ] Login (web + Android), book → start → stop → pay (Razorpay) end-to-end.
- [ ] Confirm a driver cannot read another driver's session/booking (403).
- [ ] Confirm a STOMP subscribe to someone else's `/topic/session/{id}` is denied.
- [ ] Owner sets a charging gun to MAINTENANCE → driver is billed + notified.
