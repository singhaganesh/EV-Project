# Pump Owner Dashboard — Feature Analysis & Roadmap

**Author role:** EV Infrastructure Software Architect & Product Manager
**Date:** 2026-06-15
**Scope:** The `STATION_OWNER` experience — frontend `web/src/pages/owner/*` and backend `StationController`, `EarningsController`, `AnalyticsController`, `ChargingSessionController`.

> This is an evaluation + recommendation document. It does not change code. Findings are grounded in the current source; file/line references are given so each claim is verifiable.

---

## 1. Current Features (as built)

### 1.1 Dashboard — `PumpOwnerDashboard.jsx`
- **Four KPI cards:** Active Stations (`active / total`), Energy Dispensed Today (kWh), Today's Earnings (₹), Hardware Alerts.
- **Weekly Revenue sparkline** — real 7-day trend via `fetchRevenueTrends` → `GET /api/analytics/revenue-trends/{ownerId}?days=7`.
- **Quick Actions panel:** "Add Station", "View Payouts", "Maintenance".
- **Stations Fleet list** — expandable rows drilling into dispensers/guns.
- Data source: `GET /stations/owner/{id}` + `GET /stations/owner/{id}/stats`.

### 1.2 My Stations — `MyStations.jsx`
- Search bar, Filters button, Map View button, **Add New Station** (opens `AddStationModal`).
- Three stat pills: Total Stations, Active Chargers, Utilization Rate.
- Station cards: status, address, ID, dispensary count, **car/truck ₹/kWh pricing**, open/closed, **Delete** (confirm-guarded), **Manage** → detail page.

### 1.3 Manage Station — `ManageStationPage.jsx`
- **Tab 1 — Overview & Settings:** name, address, latitude/longitude, operating hours (24h toggle or open/close dropdowns), car ₹/kWh, truck ₹/kWh, amenities (Cafe, WiFi, Restroom, CCTV, Shopping, Lounge). Persists via `PUT /stations/{id}` (amenities serialized into `meta` JSON, hours into `operatingHours`).
- **Tab 2 — Dispensers & Connectors:** add/delete dispensers; set power (kW), connector type (dynamic from `/dispensaries/connector-types`), number of guns (1–2), "accepts trucks"; per-gun **Set Maintenance / Set Available**; batched "Save Changes" with a pending-state diff.

### 1.4 Analytics — `AnalyticsPage.jsx`
- Time-range filter (Today / 7 / 14 / 30 days).
- Three efficiency KPIs: Avg Session Revenue, Avg Energy/Session, Avg Session Duration.
- **Revenue & Energy** dual-axis trend; **Revenue by Station** pie; **Revenue by Connector** pie; **Peak Usage by hour** bar chart. All real via `/api/analytics/*`.

### 1.5 Earnings — `EarningsPage.jsx`
- Four wallet cards: Current Balance, Lifetime Revenue, Pending Payouts, Last Settlement.
- Export Report button; 14-day Cash Flow trend; Station Breakdown (% contribution); paginated **Transaction Ledger** (`/api/earnings/transactions/{ownerId}`).

### 1.6 Backend surface
- **StationController:** station CRUD, `owner/{id}` list, `owner/{id}/stats`, with per-request ownership/admin authorization (`canManage`).
- **EarningsController:** summary + paginated transactions (`@PreAuthorize` owner/admin).
- **AnalyticsController:** revenue-trends, peak-usage, summary.
- **ChargingSessionController:** start/stop charging, session/booking lookups, slot/booking state machine, and **Razorpay order creation on stop**. (Driven by the customer app, but it is the source of every number the owner sees.)

---

## 2. Utility Evaluation

### 2.1 Useful / Keep (core operational value)
| Feature | Why it matters |
|---|---|
| Station & dispenser/gun CRUD | The product's backbone — owners cannot operate without asset management. |
| Per-gun Maintenance toggle | Only real "operational control" the owner has today. Prevents driver-facing failures by pulling a faulty gun offline. |
| Car/Truck ₹/kWh pricing | Direct revenue lever; truck differentiation is genuinely valuable for mixed fleets. |
| Revenue/energy trends, peak hours, station & connector splits | Real, query-backed analytics — actionable for staffing, pricing, and expansion decisions. |
| Transaction Ledger (paginated) | Audit trail / reconciliation — essential for finance and dispute handling. |
| Owner-scoped authorization | Correctly enforced server-side; prevents cross-tenant data leaks. |

### 2.2 Low-Value, Misleading, or Dead (simplify or remove)

These are ranked by **risk to owner trust**, because a dashboard that shows fake numbers is worse than one that shows fewer.

1. **Payout/Settlement figures are fabricated — highest concern.**
   `EarningsService.getEarningsSummary()` computes `currentBalance = lifetime − 85%`, `lastSettlement = lifetime × 0.15`, `pendingPayouts = last-48h revenue`. There is **no payout entity, ledger, or bank settlement** anywhere in the backend. An owner making cash-flow decisions on these numbers is being misled.
   → **Fix:** Either build a real payout ledger (see 3, High #2) or relabel these as "Gross (last 48h)" / hide Balance & Last Settlement until backed by data.

2. **"Hardware Alerts" card is hardcoded** (`value="0"`, `trendValue="-2"`). There is no alerting subsystem.
   → **Remove** until real (3, High #1), or it trains owners to ignore the one card that should save them money.

3. **Analytics KPI trend captions are hardcoded** — "Target: ₹300", "High Efficiency", "Optimal Flow" are static strings, not computed.
   → **Simplify:** drop the fake trend chips, or compute period-over-period deltas.

4. **Dead controls** (no handlers wired): Dashboard "Add Station" / "View Payouts" / "Maintenance" quick actions; My Stations **Search**, **Filters**, **Map View**, per-card "⋯" menu; Earnings **Export Report** and ledger **Search**.
   → **Either wire or hide.** Non-functional buttons erode confidence in everything else. Search/Export are cheap to implement and worth keeping (see 3, High #3 & #4).

5. **Redundant revenue surface.** Revenue-by-station appears as a pie in Analytics *and* a bar breakdown in Earnings; 7/14-day trends are repeated on Dashboard, Analytics, and Earnings.
   → **Consolidate** — let Analytics own "why" (patterns), Earnings own "how much / paid", Dashboard own "today".

6. **Minor: amenity icon mismatch** in `ManageStationPage` (`Restroom`→Car icon, `CCTV`→BatteryCharging icon). Cosmetic but looks unfinished.

---

## 3. Recommended New Features (priority-ordered by business impact)

### 🟢 HIGH — Quick wins (low effort, high impact)

**H1. Real charger uptime + fault alerting**
Replace the fake "Hardware Alerts" card with real signals derived from data you already store (`Dispensary.lastUsedTime`, `Station.lastUsedTime`, gun `MAINTENANCE` state, stalled sessions). Surface "gun X idle/unreachable", "in maintenance > 24h", "0 sessions today at an active station".
*Business case:* Charger downtime is **direct lost revenue** — a dead 60 kW gun can lose thousands of ₹/day. Turning existing timestamps into alerts is low effort and is the single highest-ROI item. Improves uptime → driver trust → occupancy.

**H2. Owner-side live session monitoring (reuse existing WebSocket)**
The infra already broadcasts slot status (`WebSocketController.notifySlotStatusChange`, used by the customer app). Subscribe the owner dashboard to show guns currently CHARGING/OCCUPIED in real time instead of a one-shot poll.
*Business case:* Real-time visibility is table-stakes for a network operator and is **mostly wiring on top of an existing channel**. Enables faster response to stuck sessions.

**H3. Working search & filter on Stations / Ledger**
Wire the already-present inputs (client-side filter to start).
*Business case:* Becomes essential the moment an owner has >5 stations; trivial effort; removes "broken app" perception.

**H4. CSV export of the Transaction Ledger**
Wire "Export Report" to a CSV download (data already returned by `/api/earnings/transactions`).
*Business case:* Owners need this for **GST filing / accounting** every month. Low effort, high retention — it makes the platform their system of record.

**H5. Net margin via electricity cost input**
Add a "grid tariff (₹/kWh)" field per station (alongside the existing price fields) and show **gross − energy cost = margin** on Analytics/Earnings.
*Business case:* Owners currently see only gross revenue. Margin visibility is the difference between a vanity dashboard and a **business tool**, and prevents owners unknowingly selling below cost. One field + one subtraction.

### 🟡 MEDIUM — Moderate effort, high value

**M1. Real payout & settlement engine**
A `Payout` entity + ledger (accrual from paid sessions, settlement batches, status, bank-ref), replacing the dummy math in `EarningsService`. Pairs with the already-captured bank details (`BusinessProfile`).
*Business case:* This is the **trust core of the financial product**. Real, reconcilable payouts are what make owners stay. Removes current legal/credibility risk of fabricated balances.

**M2. Scheduled / time-of-day dynamic pricing**
Extend pricing from a single flat rate to peak/off-peak windows (you already compute peak-usage hours).
*Business case:* Peak pricing **raises revenue on constrained hours**; off-peak discounts **fill idle capacity**. Classic yield-management lever with proven uplift, and the peak-hour data to drive it already exists.

**M3. Maintenance ticketing / fault log**
Turn the dead "Maintenance" quick action into a lightweight ticket list (open/in-progress/resolved) tied to a station/gun. Natural complement to H1.
*Business case:* Converts ad-hoc maintenance into a tracked workflow → measurable MTTR → higher uptime and accountability for field staff.

**M4. Reservation / booking visibility for owners**
Owners can't currently see upcoming/active bookings at their own sites (bookings exist, but only the customer/session side is surfaced). Add a read view + no-show/expiry stats.
*Business case:* Lets owners anticipate demand, staff appropriately, and spot no-show patterns that waste slot capacity.

### 🔵 LOW — Nice-to-have / long-term scaling

**L1. Map view** for the fleet (wire the existing button to lat/long markers). Useful at scale; lower urgency.
**L2. Staff sub-accounts / RBAC** — let an owner grant scoped access to on-site managers.
**L3. Ratings & reviews** surfaced to owners (requires a customer review system first).
**L4. Demand forecasting / smart recommendations** — ML on session history ("add a gun at Station X", "raise price 5–7 PM"). High effort, depends on data volume.
**L5. Promotions / discount codes** for off-peak demand generation.

---

## 4. Suggested Sequencing
1. **Stop the bleeding on trust:** H1 (real alerts), H5 (margin), and relabel/hide the fabricated payout figures (precursor to M1).
2. **Finish the half-built UI:** H3 (search/filter), H4 (export), H2 (live monitoring).
3. **Build the financial spine:** M1 (payouts) — unblocks credible Earnings.
4. **Grow revenue:** M2 (dynamic pricing), M3 (maintenance), M4 (bookings).
5. **Scale:** L1–L5 as the network grows.

**One-line thesis:** the dashboard *looks* complete but several headline numbers are cosmetic. The highest-ROI work is making what's already shown *true* (alerts, margin, payouts) and finishing the buttons that are already on screen — before adding anything new.
