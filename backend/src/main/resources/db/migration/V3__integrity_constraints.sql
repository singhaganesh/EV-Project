-- Integrity constraints. Each ADD CONSTRAINT is guarded so re-runs are safe.
-- NOTE: existing rows must satisfy these before enabling Flyway (clean up any
-- negative amounts, duplicate active bookings, or invalid enum strings first).

-- Money columns to exact decimal (matches the JPA NUMERIC(10,2) mapping)
ALTER TABLE payments          ALTER COLUMN amount     TYPE NUMERIC(10,2) USING amount::numeric(10,2);
ALTER TABLE charging_sessions ALTER COLUMN total_cost TYPE NUMERIC(10,2) USING total_cost::numeric(10,2);

-- One active booking per slot (prevents double-booking under failure scenarios)
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_booking_per_slot
    ON bookings (slot_id)
    WHERE status IN ('CONFIRMED', 'ONGOING');

-- Idempotency: at most one row per gateway transaction id
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_transaction_id
    ON payments (transaction_id)
    WHERE transaction_id IS NOT NULL;

-- Non-negative money
DO $$ BEGIN
    ALTER TABLE payments ADD CONSTRAINT chk_payment_amount_nonneg CHECK (amount >= 0);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE charging_sessions ADD CONSTRAINT chk_session_cost_nonneg
        CHECK (total_cost IS NULL OR total_cost >= 0);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- State of charge bounds
DO $$ BEGIN
    ALTER TABLE charging_sessions ADD CONSTRAINT chk_session_soc_bounds
        CHECK (soc_percentage IS NULL OR (soc_percentage >= 0 AND soc_percentage <= 100));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Constrain enum-backed VARCHAR columns to the valid value sets
DO $$ BEGIN
    ALTER TABLE charger_slots ADD CONSTRAINT chk_slot_status
        CHECK (status IN ('AVAILABLE','RESERVED','BOOKED','CHARGING','PAYMENT_PENDING','MAINTENANCE','OCCUPIED'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE bookings ADD CONSTRAINT chk_vehicle_type
        CHECK (vehicle_type IS NULL OR vehicle_type IN ('CAR','TRUCK'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
