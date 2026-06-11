-- Indexes supporting the hot dashboard/availability queries.
-- IF NOT EXISTS so this is safe to apply to an existing schema.

-- Map discovery: bounding-box queries on station coordinates
CREATE INDEX IF NOT EXISTS idx_station_lat_lng ON stations (latitude, longitude);

-- Owner dashboard: "fetch all my stations"
CREATE INDEX IF NOT EXISTS idx_station_owner ON stations (owner_id);

-- "fetch all my bookings"
CREATE INDEX IF NOT EXISTS idx_booking_user ON bookings (user_id);

-- Availability check by slot + status
CREATE INDEX IF NOT EXISTS idx_booking_slot_status ON bookings (slot_id, status);

-- Payment-pending / unpaid cleanup scans by end time
CREATE INDEX IF NOT EXISTS idx_session_endtime ON charging_sessions (end_time);
