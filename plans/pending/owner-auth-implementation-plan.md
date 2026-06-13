# Implementation Plan: Pump Owner Registration, Verification & MFA Login

**Source spec:** [`owner-auth-spec.md`](./owner-auth-spec.md)
**Date:** 2026-06-13
**Status:** Approved approach, not yet implemented (no code changed).

This plan implements verified Station Owner (Pump Owner) registration, email
verification, admin approval, and email-based MFA login, per the decisions below.

---

## Decisions (confirmed with stakeholder)

| # | Topic | Decision |
|---|-------|----------|
| 1 | Email delivery | **Gmail SMTP** (App Password supplied; stored as `GMAIL_APP_PASSWORD`, never committed) |
| 2 | Document storage | **Supabase Storage REST API** called directly from the backend (no SDK) |
| 3 | Flyway | **Enable now** (option b) — switch dev to `validate`, let Flyway own the schema |
| 4 | MFA scope | **Owners only** — admins/customers keep the current direct-token login |
| 5 | OTP / temp-token store | **Redis (Upstash)** — `rediss://` TLS URL in `application-dev.properties` |
| 6 | Frontend | **Build it** — multipart owner registration + OTP step, login MFA step, dev autofill |

---

## Findings from the current codebase

- `application-dev.properties` is **gitignored** and already contains a `spring.mail.*`
  block + `app.auth.otp-expiration-minutes` / `app.auth.mfa-enabled`. But
  `spring.mail.username` is still the placeholder `your-system-email@gmail.com`.
- Dev uses `spring.jpa.hibernate.ddl-auto=create` (wipes & recreates schema each boot).
  This **conflicts** with enabling Flyway — resolved in Phase 2.
- `AuthController` already has `POST /api/auth/register` (simple JSON, even handles
  `role:"PUMP_OWNER"`) and `POST /api/auth/login` (email+password, BCrypt,
  `LoginAttemptService` lockout). The web `RegisterPage`/`LoginPage` use these today —
  the MFA work changes the `/login` contract and replaces the web register flow.
- Existing `OtpService` is **mobile/SMS + DB** oriented (`Otp` entity). The new email
  OTP + temp-login-token store will be **Redis-based and separate**, leaving the working
  app login untouched.
- `User` has `email`, `password`, `role` (CUSTOMER/STATION_OWNER/ADMIN) but **no**
  `status`/`mfa_enabled`/`mfa_secret` — added in Phase 4.
- No `AdminController` / `UserController` exists — the approval endpoint is new.
- Method security (`@EnableMethodSecurity`) is already on; `/api/auth/**` is `permitAll`,
  `/api/admin/**` falls under "authenticated" + `@PreAuthorize`.
- Web `authSlice` stores `user` in `localStorage`, token via `tokenStorage`; `LoginPage`
  expects `{token, user}` and redirects by role.

---

## ⚠️ Security & inputs

- The Gmail App Password and Redis token were shared in chat — they live only in the
  gitignored dev file / env vars, never tracked code, but should be **rotated** after
  bring-up.
- **Still required before the app can run end-to-end:**
  1. The **Gmail sender address** the App Password belongs to (password used as
     `ypdvnqxhrrwgdujz`, spaces stripped).
  2. **Supabase backend credentials**: a **service-role key** (env var), confirmation the
     URL is `https://pjaydfyqtlubpmtrlsad.supabase.co`, and that the `business-documents`
     bucket exists (or steps to create it + RLS policies).

---

## Phases

### Phase 1 — Dependencies (`pom.xml`)
- Add `spring-boot-starter-mail`.
- Add `spring-boot-starter-data-redis`.
- No Supabase SDK — use Spring `RestClient`/`WebClient` against the Storage REST API.

### Phase 2 — Configuration & the Flyway / ddl-auto conflict
- **dev** (`application-dev.properties`): set real `spring.mail.username`; add
  `spring.data.redis.url=rediss://...upstash.io:6379` (TLS via `rediss://`, required by
  Upstash); add `supabase.url` / `supabase.service-key` / `supabase.bucket=business-documents`.
- **Resolve conflict:** switch dev to `ddl-auto=validate` + `spring.flyway.enabled=true`
  (matching prod). **Risk:** dev DB was built by Hibernate, so `baseline-on-migrate=true`
  baselines at V1 then replays V2, V3, V4. Verify `V2__add_indexes.sql` /
  `V3__integrity_constraints.sql` are idempotent (`IF NOT EXISTS`) first; if not, drop the
  dev schema once so Flyway builds it fresh from V1.
- **prod** (`application-prod.properties`): add `spring.mail.*`, `spring.data.redis.*`,
  `supabase.*` as `${ENV_VAR}` with no defaults (matches the no-committed-secrets hardening).

### Phase 3 — Flyway migration `V4__owner_auth.sql`
- Add `status`, `mfa_enabled`, `mfa_secret` to `users`; backfill existing rows to
  `APPROVED`; create `business_profiles` (FK + `ON DELETE CASCADE`) — exactly per spec.
- Default `status='APPROVED'` keeps existing customers/admins working; new owner signups
  override to `PENDING_EMAIL_VERIFICATION` in code.

### Phase 4 — Domain model
- `User`: add `UserStatus status` (enum: `PENDING_EMAIL_VERIFICATION`,
  `PENDING_ADMIN_APPROVAL`, `APPROVED`, `SUSPENDED`), `Boolean mfaEnabled`, `String mfaSecret`.
- New `BusinessProfile` entity (one-to-one with `User`) + `BusinessProfileRepository`.

### Phase 5 — Redis-backed OTP & temp-token store
- New `MfaOtpService` (separate from the existing DB/mobile `OtpService`). Redis keys w/
  5-min TTL: `regotp:{userId}`, `mfaotp:{tempToken}` (hashed OTP), `templogin:{tempToken}→userId`.
- Reuse `LoginAttemptService` for lockout on the email endpoints.

### Phase 6 — Email sender service
- `EmailService` over `JavaMailSender` to send the 6-digit OTP. Honors
  `otp.expose-in-response` so dev can read the OTP from the JSON response.

### Phase 7 — Supabase Storage service
- `DocumentStorageService.upload(userId, type, file)` → PUT to
  `…/storage/v1/object/business-documents/owner-{userId}/{type}_{ts}.{ext}` with the
  service-role bearer token; returns the stored object path. Dev bypass: accept tiny
  placeholder files (existence-only validation).

### Phase 8 — Auth endpoints (extend `AuthController`)
- `POST /api/auth/register/owner` (multipart): validate email unique → create `User`
  (STATION_OWNER, `PENDING_EMAIL_VERIFICATION`) → upload 3 docs → save `BusinessProfile`
  → generate+email OTP → return `{userId}`.
- `POST /api/auth/verify-registration`: validate OTP; dev → `APPROVED`,
  prod → `PENDING_ADMIN_APPROVAL`.
- `POST /api/auth/verify-mfa`: validate temp token + OTP → issue JWT + refresh token via
  `JwtUtil` + `RefreshTokenService`.

### Phase 9 — MFA login (modify existing `POST /api/auth/login`) — owners only
- After password check + `APPROVED` assertion (`403` if `PENDING_ADMIN_APPROVAL`):
  - `role == STATION_OWNER` && `mfaEnabled` → temp token + email OTP, return
    `{mfaRequired:true, tempLoginToken}`.
  - else (admins, customers, mfa off) → return `{token, user}` as today.

### Phase 10 — Admin approval endpoint
- New `AdminController`: `PUT /api/admin/users/{userId}/approve` with
  `@PreAuthorize("hasRole('ADMIN')")`; assert target is STATION_OWNER → set `APPROVED`.
  No SecurityConfig change needed beyond confirming `/api/admin/**` is authenticated.

### Phase 11 — Frontend (React `web/`)
- `RegisterPage.jsx`: replace JSON `/auth/register` with multipart `/auth/register/owner`
  (company, taxId, phone, bank account + IFSC, 3 file inputs) → OTP step hitting
  `/auth/verify-registration`. Add **dev-only autofill** button (`import.meta.env.DEV`)
  with mock blobs.
- `LoginPage.jsx`: handle both response shapes — if `mfaRequired`, show OTP step →
  `/auth/verify-mfa` → `setCredentials`. Admin path unchanged.
- `authSlice` / `axios`: add handling for pending/temp-token states; no structural change.

### Phase 12 — Verification
- `mvn verify` (compile + context load) and `npm run build`.
- Dev run-through: register owner (autofill) → read OTP from response → verify → login →
  MFA OTP → dashboard. Optionally drive live via the `/run` skill.

---

## Build order
1 → 2 → 3 → 4 (foundation must compile against new schema) → 5/6/7 (infra services, parallelizable)
→ 8/9/10 (endpoints) → 11 (frontend) → 12 (verify).
