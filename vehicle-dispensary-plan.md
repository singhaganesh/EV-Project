# Vehicle Types & Dispensaries Plan

## Goal
Implement a Car/Truck selection workflow, upgrade the station hierarchy to include Dispensaries (with two guns and shared power), enforce a 15-minute booking expiry window, and create a sophisticated "Add Station" form for pump owners.

## Context
As the `project-planner` using the `brainstorming` and `plan-writing` skills, I have inspected the existing `Station`, `ChargerSlot`, and `Booking` models in the backend, as well as the React web application (`MyStations.jsx`). 

## Tasks

### 1. Backend Database Schema Upgrades
- [ ] Task 1: Create `VehicleType` enum (`CAR`, `TRUCK`).
- [ ] Task 2: Create `Dispensary` entity (id, station_id, name, totalPowerKw, acceptsTrucks).
- [ ] Task 3: Update `ChargerSlot` entity to reference `Dispensary` instead of `Station` directly (or in addition to). Ensure it represents 1 of the 2 guns.
- [ ] Task 4: Update `Booking` entity to include `VehicleType`, `expiresAt` (createdAt + 15 mins), and `actualStartTime`.
- [ ] Task 5: Add `truckPricePerKwh` (or multiplier) to `Station` entity.
- [ ] Task 6: Run Schema Validator / create migration script.

### 2. Backend API Logic
- [ ] Task 1: Update Booking creation logic: set expiry to 15 mins from now. Allow `vehicleType` selection.
- [ ] Task 2: Add Spring `@Scheduled` task to find and CANCEL bookings older than 15 minutes that haven't started.
- [ ] Task 3: Update Charging Session logic: when started before expiry, update the `actualStartTime` on `Booking`. On stop, calculate cost based on vehicle type pricing.
- [ ] Task 4: Update Station Creation API to accept nested Dispensaries (and automatically generate 2 `ChargerSlot` items per dispensary).

### 3. Web UI: Pump Owner Portal
- [ ] Task 1: Build `AddStationModal.jsx` (or a dedicated page) in `web/src/pages/owner/`.
- [ ] Task 2: Form Step 1: Base Station Info (Name, Lat/Lng Map Picker, Car Price/kWh, Truck Price/kWh).
- [ ] Task 3: Form Step 2: Dynamic Dispensary List. "Add Dispensary" button. Each dispensary form sets Total Power (kW) and a checkbox for "Accepts Trucks".
- [ ] Task 4: Integration with Backend API (`POST /api/stations`).

### 4. Android UI: Booking Flow
- [ ] Task 1: Update `SlotBookingScreen.kt`. Add UI toggle for "Car" or "Truck".
- [ ] Task 2: Disable slot if "Truck" is chosen but the slot's dispensary doesn't accept trucks.
- [ ] Task 3: Update `MyBookingsScreen.kt` to show the 15-minute countdown timer.

## Done When
- [ ] Pump Owner can create a new station with multiple dispensaries, checking "Accepts Trucks" appropriately.
- [ ] Android App successfully books a slot specifying Car or Truck with correct pricing.
- [ ] Booking automatically cancels if the "Start Charging" button isn't clicked within 15 minutes.
- [ ] Starting the charge records the actual start time, overriding the original requested block.
