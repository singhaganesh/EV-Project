# Realtime (STOMP/WebSocket) Contract

Shared contract for the Android driver app, the React owner dashboard, and the
Spring backend. Keep all three aligned with this document.

## Connection

- **Endpoint:** `/ws` (SockJS enabled).
- **Auth (required):** send the JWT in the STOMP `CONNECT` frame as
  `Authorization: Bearer <token>`. The backend `JwtChannelInterceptor` rejects
  CONNECT without a valid token and authorizes every `SUBSCRIBE` by ownership.

## Destinations

| Destination | Who may subscribe | Purpose |
|---|---|---|
| `/topic/session/{bookingId}` | the booking's owner (or ADMIN) | private driver telemetry |
| `/topic/station/{stationId}` | any authenticated user | public aggregate (for searchers) |
| `/topic/owner/station/{stationId}` | the station's owner (or ADMIN) | owner health metrics |
| `/topic/station/{stationId}/slots` | any authenticated user | slot status changes |
| `/topic/user/{userId}/bookings` | that user (or ADMIN) | booking lifecycle updates |

## Payload schemas (Jackson camelCase JSON)

### `/topic/session/{bookingId}` — driver telemetry
```
bookingId: long, slotId: long, stationId: long,
powerKw: double, energyDispensedKwh: double, socPercentage: double,
totalCost: double, minutesRemaining: double,
maxPowerKw: double, batteryCapacityKwh: double, pricePerKwh: double
```

### `/topic/station/{stationId}` — public aggregate
```
slotId: long, status: "CHARGING", soc: int (rounded), timeLeft: int (minutes)
```

### `/topic/owner/station/{stationId}` — owner health
```
slotId: long, temp: string ("28.5°C"), voltage: int, current: int,
power: double, energy: double, status: "OPERATIONAL" | "CRITICAL_HEAT"
```

### `/topic/user/{userId}/bookings` — booking updates
```
bookingId: long, status: string
```
Maintenance force-stop variant:
```
bookingId: long, status: "FORCE_STOPPED_MAINTENANCE",
message: string, totalCost: double, razorpayOrderId: string
```

### `/topic/station/{stationId}/slots` — full ChargerSlot
Note the JSON property renames on `ChargerSlot`:
- `slotLabel` is serialized as **`slotNumber`**
- `powerKw` is serialized as **`powerRating`**

## Enum string values (must match byte-for-byte across clients)

- `SlotStatus`: `AVAILABLE`, `RESERVED`, `BOOKED`, `CHARGING`, `PAYMENT_PENDING`, `MAINTENANCE`, `OCCUPIED`
- `VehicleType`: `CAR`, `TRUCK`
- `BookingStatus`: `CONFIRMED`, `ONGOING`, `COMPLETED`, `CANCELLED`, `EXPIRED`
