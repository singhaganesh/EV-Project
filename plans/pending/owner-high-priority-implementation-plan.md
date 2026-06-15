# Owner Dashboard — High-Priority Implementation Plan

**Scope:** H2 (live monitoring), H3 (search/filter), H4 (CSV export), H5 (net margin), M4 (booking visibility), + payout relabel.
**Date:** 2026-06-15
**Status:** Pending — not yet implemented.

## Ground truth (verified in code)
- DB schema is `spring.jpa.hibernate.ddl-auto=update`, Flyway disabled → new entity fields auto-add as columns. No manual migration required (optional Flyway script for parity).
- WebSocket: backend broadcasts full `ChargerSlot` to `/topic/station/{stationId}/slots` (`WebSocketController.notifySlotStatusChange`). Endpoint `/ws` (SockJS). `JwtChannelInterceptor` requires `Authorization: Bearer <token>` in the STOMP CONNECT frame. Web has **no** STOMP client yet.
- Sessions are simulator-driven (`ChargingSimulatorService`) → live monitoring works without hardware.
- Earnings: `EarningsService` fabricates `currentBalance` (15% of lifetime) and `lastSettlement` (15%); `pendingPayouts` = real last-48h revenue. `getTransactionHistory` is paginated only.
- Bookings: no owner-scoped query/endpoint (`BookingRepository` has user-scoped only).
- Station already has `pricePerKwh` / `truckPricePerKwh`; we add `costPerKwh`.

## Build order (dependency-aware)
Phase 0 deps → Phase 1 (H5 + payout relabel) → Phase 2 (H3) → Phase 3 (H4) → Phase 4 (M4) → Phase 5 (H2). Data/value first; real-time last (most moving parts). Each phase builds + verifies before the next.

---

## Phase 0 — Dependencies
1. `cd web && npm i @stomp/stompjs sockjs-client` (only needed by H2, install once up front).
2. Confirm where the JWT is stored for the authed client (inspect `web/src/api/axios.js`) — STOMP CONNECT needs the raw token. Note the exact accessor for Phase 5.

**Verify:** `npm run build` still succeeds.

---

## Phase 1 — H5 Net Margin + Payout Relabel (backend-first)
Folds the fake-payout cleanup into the margin work since both live in `EarningsService`.

### Backend
1. `model/Station.java` — add `@Column(columnDefinition="DOUBLE PRECISION DEFAULT 0.0") private Double costPerKwh;` (grid tariff ₹/kWh).
2. `repository/ChargingSessionRepository.java` — add:
   `@Query("SELECT COALESCE(SUM(s.energyKwh * COALESCE(s.booking.slot.station.costPerKwh,0)),0) FROM ChargingSession s WHERE s.booking.slot.station.owner.id = :ownerId AND s.paymentStatus = 'PAID'")`
   `Double getTotalEnergyCost(@Param("ownerId") Long ownerId);`
3. `dto/EarningsSummaryDTO.java` — replace the fabricated fields. New honest shape:
   `lifetimeRevenue`, `energyCost`, `netMargin` (= lifetime − energyCost), `revenueLast48h` (rename of pendingPayouts).
4. `service/EarningsService.java` — delete the 85%/15% dummy math; populate the new DTO from real queries.

### Frontend
5. `pages/owner/ManageStationPage.jsx` — in "Operations & Pricing" add a "Grid Tariff (₹/kWh)" `InputField` bound to `station.costPerKwh` (`handleNumberChange('costPerKwh')`); already saved via the existing `PUT /stations/{id}` payload spread.
6. `store/earningsSlice.js` — update `initialState.summary` keys to the new DTO; fix the loading guard in EarningsPage that reads `summary.lifetimeRevenue`.
7. `pages/owner/EarningsPage.jsx` — replace the 4 wallet cards with honest ones: **Lifetime Revenue**, **Energy Cost**, **Net Margin**, **Revenue (Last 48h)**. Remove "Current Balance" / "Last Settlement" / "Pending Payouts" labels.

**Benefit:** Owners see real profitability instead of gross-only and fabricated balances. **Verify:** backend `./mvnw -q compile`; `npm run build`; load Earnings → margin = lifetime − (energy × tariff); set a tariff in Manage Station and confirm it persists + flows through.

---

## Phase 2 — H3 Search & Filter
### Stations (client-side, list already in state)
1. `pages/owner/MyStations.jsx` — add `query` state to the existing search input; derive `filteredStations` (name/address/`#id`, case-insensitive); map over filtered list.

### Ledger (server-side, paginated)
2. `repository/ChargingSessionRepository.java` — add an overloaded `getTransactionHistory(ownerId, search, pageable)` with `AND (:search IS NULL OR LOWER(s.booking.slot.station.name) LIKE LOWER(CONCAT('%',:search,'%')) OR CAST(s.razorpayOrderId AS string) LIKE CONCAT('%',:search,'%'))`.
3. `service/EarningsService.java` + `controller/EarningsController.java` — thread an optional `@RequestParam(required=false) String search`.
4. `api/axios.js` + `store/earningsSlice.js` — pass `search` through `fetchEarningsTransactions`; reset to page 0 on new search (debounced input).
5. `pages/owner/EarningsPage.jsx` — wire the existing ledger search input.

**Benefit:** Usable past ~5 stations; removes dead-input feel. **Verify:** filter stations by typing; ledger search returns matching rows + correct pagination.

---

## Phase 3 — H4 CSV Export
### Backend
1. `repository/ChargingSessionRepository.java` — add non-paged `List<TransactionRowDTO> getAllTransactions(ownerId)` (same projection, no Pageable).
2. `controller/EarningsController.java` — `GET /api/earnings/transactions/{ownerId}/export` → `produces="text/csv"`, `@PreAuthorize` owner/admin, `Content-Disposition: attachment; filename=transactions.csv`. Build CSV from the DTO list (header + rows; escape commas/quotes).

### Frontend
3. `api/axios.js` — export helper requesting the endpoint with `responseType:'blob'`.
4. `pages/owner/EarningsPage.jsx` — wire the existing "Export Report" button to trigger the blob download (objectURL + anchor click).

**Benefit:** Monthly GST/accounting export → platform becomes their system of record (retention). **Verify:** button downloads a valid CSV opening cleanly in Excel/Sheets; rows match the ledger.

---

## Phase 4 — M4 Owner Booking Visibility
### Backend
1. `repository/BookingRepository.java` — add `Page<Booking> findBySlotStationOwnerId(Long ownerId, Pageable pageable)` (+ optional `findBySlotStationOwnerIdAndStatus`).
2. `service/BookingService.java` — owner-scoped fetch method.
3. `controller/BookingController.java` — `GET /api/bookings/owner/{ownerId}` with the same owner/admin authorization pattern used in `StationController` (compare principal → user id, allow ADMIN). Paged, sorted by `startTime desc`.

### Frontend
4. New `pages/owner/BookingsPage.jsx` — table of upcoming/active/past bookings (driver, station, slot, vehicle type, start/end, status badge), status filter, pagination. Reuse `DataTable` + `StatusBadge`.
5. `store/bookingsSlice.js` (new) — thunk + selectors; register in the store.
6. Add route + sidebar nav entry for `/owner/bookings`.

**Benefit:** Owners can anticipate demand, staff sites, and spot no-show/expiry patterns. Pure read over existing data. **Verify:** page lists this owner's bookings only; cross-owner returns 403; filters/pagination work.

---

## Phase 5 — H2 Live Session Monitoring
### Frontend (backend already broadcasts)
1. New `web/src/api/socket.js` — STOMP-over-SockJS singleton: `connect(token)` sets the `Authorization` CONNECT header; `subscribeStationSlots(stationId, cb)`; `disconnect()`. Use the token accessor confirmed in Phase 0.
2. `pages/owner/PumpOwnerDashboard.jsx` (and/or `ManageStationPage` Dispensers tab) — on mount, connect and subscribe to `/topic/station/{id}/slots` for each owned station; merge incoming `ChargerSlot` (note JSON renames: `slotLabel`→`slotNumber`, `powerKw`→`powerRating`) into local slot state so gun status updates live (`CHARGING`/`AVAILABLE`/`MAINTENANCE`…). Clean up subscriptions on unmount.
3. Add a small "Live Sessions" panel: guns currently `CHARGING` with station/gun label (energy/elapsed if available from payload).

**Benefit:** Real-time operational view (driven by the simulator today, hardware-ready later); faster reaction to stuck/idle chargers. **Verify:** start a simulated session (customer flow) → owner dashboard reflects the gun going `CHARGING` then back without manual refresh; confirm CONNECT is rejected without a valid JWT.

---

## Cross-cutting / risks
- **Auth on every new endpoint:** mirror existing owner/admin checks (`StationController.canManage`, `EarningsController @PreAuthorize`). No new endpoint should trust a path id without verifying the principal.
- **DTO change blast radius:** renaming `EarningsSummaryDTO` fields touches the slice + page together — do them in the same commit (Phase 1) to avoid a broken build.
- **costPerKwh nulls:** `COALESCE` in the query; default 0.0 column so legacy rows don't break margin.
- **STOMP token:** if the JWT lives in `sessionStorage`, ensure the socket reads it the same way axios does; reconnect after token refresh.
- **Optional:** add a Flyway script `db/migration/V_add_station_cost_per_kwh.sql` for schema parity even though `ddl-auto=update` handles dev.

## Suggested commits (one per phase)
1. `feat(owner): net margin + honest earnings summary (H5 + payout relabel)`
2. `feat(owner): station & ledger search/filter (H3)`
3. `feat(owner): CSV export of transaction ledger (H4)`
4. `feat(owner): owner booking visibility page (M4)`
5. `feat(owner): live charger/session monitoring via WebSocket (H2)`

## Verification gate (run after each phase)
- Backend: `cd backend && ./mvnw -q compile`
- Frontend: `cd web && npm run build`
- Manual smoke per the "Verify" note in each phase.
