# Missing-Features Audit — EV Charging Management System ("Plugsy")

**Date:** 2026-06-12
**Scope note:** Physical charger (OCPP) hardware integration is **intentionally out of scope** —
the project does not have access to physical chargers, so charging is driven by the in-house
`ChargingSimulatorService` by design. This audit therefore excludes hardware integration and
focuses on gaps that matter for the simulator-based product.

**Baseline (verified in code):**
- **Android driver app** — 14 screens: splash, onboarding, OTP login, home/map search, station
  detail, slot booking, booking confirmation/detail, my bookings, live charging (STOMP telemetry),
  charging history, payment summary/success, profile.
- **React owner/admin dashboard** — owner: dashboard, my stations, manage station, analytics,
  earnings; admin: dashboard overview, stations list.
- **Spring Boot backend** — OTP auth + refresh tokens, bookings (15-min expiry, ownership
  enforcement), charging sessions (simulator + crash recovery), idempotent Razorpay payment
  verification, stations/dispensaries/slots, geocoding proxy, IoT telemetry, analytics
  (revenue trends, peak usage, summary), earnings (summary, transactions), station
  recommendation scoring, JWT-secured STOMP realtime.

**Benchmarks:** Statiq, ChargeZone, Tata Power EZ Charge, Kazam (India); ChargePoint, EVBox,
PlugShare (global).

---

## Gap Table

| # | Feature | Category | Gap description | Priority | Suggested implementation |
|---|---------|----------|-----------------|----------|--------------------------|
| 1 | **SMS OTP delivery** | Core workflow | No SMS gateway exists; in prod the OTP is generated, hashed, and never delivered — **login is impossible in production**. Already flagged in `docs/PRODUCTION_CHECKLIST.md` but still open. | **High** — blocks 100% of real users at the front door | MSG91 or Twilio Verify behind an `OtpSender` interface in `OtpService`; keep the dev-profile response-exposure flag as the local fallback |
| 2 | **Push notifications (FCM)** | Mobile experience | `POST_NOTIFICATIONS` is declared in the manifest, but there is no Firebase/FCM dependency anywhere. All alerts (charge complete, slot expiring, force-stop billing) arrive only over WebSocket **while the app is foregrounded**. The README roadmap promised this for April 2026 — it's June. | **High** — a driver who walks away from the car never learns charging finished; direct churn driver | Add FCM: device-token registry on `User`, send on the existing notification points (`ChargerSlotService.forceStopActiveSession`, booking expiry scheduler, session completion) |
| 3 | **Refunds & cancellation policy** | Core workflow / Payments | Zero refund code in the backend. Razorpay payments are capture-only; a disputed/double payment or a paid-then-faulted session has no remediation path. No cancellation-fee logic either. | **High** — payment disputes without refund tooling become Razorpay chargebacks and support nightmares | `POST /api/payments/refund/{paymentId}` (admin/owner-gated) wrapping Razorpay's refund API; add `REFUNDED`/`PARTIALLY_REFUNDED` to `Payment.PaymentStatus`; define a cancellation-fee window in booking config |
| 4 | **Owner payouts / settlement** | Core workflow / Integrations | `EarningsService` computes owner earnings, but money only flows **in** to the platform's Razorpay account. There is no settlement mechanism, ledger, or payout schedule — the marketplace's other half is missing. | **High** — owners onboard only if they can get paid; this is the business model | Integrate Razorpay Route (split payments at capture time) or scheduled payouts via RazorpayX; add a `Settlement` entity reconciled against `Payment` rows |
| 5 | **Test suite & CI/CD** | Reliability | One test file exists — the default Spring context-load test. No `.github/workflows`. The README's claims of JUnit/Mockito/GitHub Actions describe an aspiration, not the repo. Every one of the 32 recent security fixes is regression-unprotected. | **High** — the codebase just got 32 hand-verified fixes; without tests they erode silently | Start with the highest-value seams: `BookingService` overlap/expiry, `PaymentController` verify idempotency, `JwtChannelInterceptor` subscribe authorization; one GitHub Actions workflow running `mvn verify` + `npm run build` on PR |
| 6 | **Ratings & reviews (write path)** | User-facing | `Station.rating` is a plain column that `StationRecommendationService` *reads* for scoring, but nothing ever *writes* it — no `Review` entity, no submission endpoint, no UI. The recommendation engine is ranking on permanently-zero data. | **Medium** — trust signals drive station choice in every competitor (PlugShare's moat is reviews) | `Review` entity (user, station, stars, text, photo) gated on a `COMPLETED` session; recompute `Station.rating` on write; surface in `StationDetailScreen` |
| 7 | **Admin console depth** | Security / Roles | Web admin has exactly two pages (`DashboardOverview`, `StationsList`). No user management, no owner approval/KYC queue, no payment/refund browser, no force-actions. Admin role exists in the backend but has almost no surface. | **Medium** — every operational incident currently requires SQL access | Add admin pages backed by mostly-existing endpoints: user list/suspend, owner verification queue, payments table with refund button (pairs with #3) |
| 8 | **Owner KYC / verification** | Onboarding / Security | Any registered OWNER can create stations that drivers immediately see and pay at. No document upload, GST/PAN capture, or approval gate — a fraudulent "station" can collect bookings. | **Medium** — fraud + compliance exposure grows with each onboarded owner; prerequisite for payouts (#4) | `OwnerProfile` with KYC fields + `PENDING_VERIFICATION` station state excluded from search until admin approval (consumes the queue from #7) |
| 9 | **Rate limiting / abuse protection** | Security | No rate limiting anywhere (`Bucket4j`/filter absent). `send-otp` can be hammered to bomb SMS costs once #1 lands; booking endpoints can be scripted to lock every slot in a city for free. | **Medium** — cheap to add now, expensive after the first abuse incident | Bucket4j filter: tight per-phone + per-IP limits on `/api/auth/send-otp`, moderate global limits elsewhere; CAPTCHA fallback on web register |
| 10 | **Account deletion & data export** | Security / Compliance | No delete-account or export path in API or apps. India's DPDP Act 2023 grants erasure rights, and **Google Play requires an account-deletion URL** for apps with accounts — this can block store listing. | **Medium** — store-listing blocker, low build cost | `DELETE /api/users/me` (soft-delete + PII anonymization, retain financial rows for tax law), surfaced in `ProfileScreen` + a web URL for the Play Console field |
| 11 | **Receipts / GST invoices** | User-facing / Compliance | Payment succeeds → `PaymentSuccessScreen` → nothing. No emailed receipt, no downloadable invoice. B2B truck-fleet customers (the stated differentiating segment) need GST invoices to claim input credit. | **Medium** — table stakes in India for the truck/fleet segment | PDF invoice generation (owner GSTIN from #8, HSN/SAC code, tax breakup) attached to a transactional email; "Download invoice" on history screens |
| 12 | **Monitoring & alerting** | Reliability | No Actuator/health/metrics config, no error tracking. README lists Monitoring as "TBD". First sign of a prod outage will be a user complaint. | **Medium** — you can't run a 24/7 charging network blind | Spring Boot Actuator + Micrometer→Prometheus/Grafana (or a hosted APM); Sentry SDK in React and Android; uptime check on `/actuator/health` |
| 13 | **Web accessibility (WCAG)** | Accessibility | **Zero** `aria-*` or explicit `role=` attributes across `web/src`. MUI provides some baseline, but custom owner-dashboard components (charts, slot grids, map picker) are invisible to screen readers; charts have no text alternatives. | **Medium** — legal/procurement requirement if dashboards are sold to corporate fleet operators | Audit with axe-core; add labels to icon buttons, `aria-live` for realtime slot updates, data-table fallbacks for charts, visible focus states |
| 14 | **Android accessibility** | Accessibility | Partial: 42 `contentDescription`s across 15 files is a start, but key interactive flows (charging telemetry gauges, slot picker) need TalkBack verification; no large-font/contrast testing evident. | **Low** — foundation exists; needs an audit pass, not a rebuild | Run Accessibility Scanner + TalkBack pass on the booking→charging→payment funnel; add `semantics{}` blocks to custom Compose components |
| 15 | **Trip/route planning** | User-facing | Search is "what's near me now". No route-based charger planning (origin→destination with charge stops), the #1 feature of PlugShare/ABRP and especially relevant for the truck segment doing inter-city haulage. | **Low** — differentiating but only after the basics (#1–#4) work | Phase 1: corridor search — stations within X km of a Directions API polyline; defer SoC-aware stop optimization |
| 16 | **Favorites & recents** | User-facing | No favorites/saved-stations anywhere (verified zero matches). Repeat usage (daily commuters, fleet routes) re-searches every time. | **Low** — small effort, retention win | `user_favorite_station` join table + heart icon on `StationCard`/`StationDetailScreen`; "Recent stations" from booking history needs no schema at all |
| 17 | **Recurring bookings & subscriptions** | User-facing / Roadmap | README roadmap commits to recurring bookings (June 2026 — now) and is considering subscriptions. Nothing in the schema supports either (no recurrence fields, no plan entity). | **Low** — roadmap item, but sequence it after payments are fully closed-loop (#3, #4) | `BookingTemplate` (slot, weekday, time) materialized into real bookings T-24h by the existing scheduler infra |
| 18 | **iOS / driver web app** | Cross-platform | Drivers are Android-only; the web app serves only owners/admins. In metro India, iPhone share among the EV-buying demographic is significant and entirely unserved. | **Low** — major investment; validate on Android first | Nearest-term: a mobile-web driver booking flow reusing the React stack + existing APIs; evaluate KMP/Compose Multiplatform later given the Kotlin codebase |
| 19 | **Localization (i18n)** | Inclusive design | No locale infrastructure observed in either client (single `strings.xml` set, no i18n lib in web). English-only excludes a large share of Indian drivers, particularly truck drivers in the target segment. | **Low** — important for the truck segment, but staffing translations is the real cost | Externalize strings now (cheap while the app is small): Android resource qualifiers, `react-i18next` on web; start with Hindi |

---

## Executive Summary — Top 3 Gaps & Next Steps

### 1. The product cannot deliver a login code or notify a user (#1, #2)
OTPs are generated but never sent, and alerts die when the app is backgrounded. With hardware
integration deliberately out of scope, these two gaps are what stand between the current build
and a usable pilot — and push notifications (per the README roadmap) are already past their
committed date.
**Next step:** treat SMS (days of work) and FCM (about a week) as the immediate sprint.

### 2. Money flows in but never out or back (#3, #4)
There are no refunds and no owner settlements. The first payment dispute and the first owner
asking "where's my money?" are both unanswerable today — and both will happen in week one of
any pilot.
**Next step:** wire Razorpay's refund API behind an admin action (small, contained), and make
a strategic call on Razorpay Route (split at capture) vs. batch payouts **before** onboarding
any external station owner, because it dictates the money-ledger design.

### 3. Zero regression protection on a freshly-hardened codebase (#5)
Thirty-two security and correctness fixes were just landed by hand with no tests and no CI;
their guarantees (payment idempotency, booking ownership, WebSocket subscription auth) will
quietly rot.
**Next step:** one GitHub Actions workflow plus ~15 targeted tests on the booking/payment/auth
seams — roughly two days of work that protects everything already paid for.

---

## Suggested Sequencing

| Sprint | Items | Theme |
|--------|-------|-------|
| Sprint 1 | #1 SMS, #2 FCM, #5 tests + CI | Make the pilot possible and protected |
| Sprint 2 | #3 refunds, #9 rate limiting, #10 account deletion | Play-Store and abuse blockers |
| Sprint 3 | #4 payouts → #8 KYC → #7 admin console | Owner-marketplace track |
| Post-pilot backlog | #6, #11–#19 | Trust, compliance polish, growth features |
