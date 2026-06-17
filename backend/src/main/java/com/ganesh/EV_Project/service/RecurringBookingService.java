package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.BookingRequest;
import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.VehicleType;
import com.ganesh.EV_Project.model.BookingTemplate;
import com.ganesh.EV_Project.repository.BookingTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Materializes recurring-booking templates (G2).
 *
 * Every minute, for each active template whose weekday matches today and whose
 * scheduled time is within the next {@link #LEAD_MINUTES} minutes, it creates a
 * normal booking via {@link BookingService} (same flow/hold as instant booking)
 * and pushes the outcome. {@code lastMaterializedDate} is stamped before the
 * attempt so each template is tried at most once per day (no retry spam, no
 * double-booking).
 *
 * Not {@code @Transactional} at the method level on purpose: the stamp is
 * committed independently so a failed {@code createBooking} (its own
 * transaction) cannot roll it back and cause repeated attempts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringBookingService {

    private static final int LEAD_MINUTES = 20;

    private final BookingTemplateRepository templateRepository;
    private final BookingService bookingService;
    private final PushNotificationService pushNotificationService;

    @Scheduled(fixedRate = 60000)
    public void materializeDueTemplates() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        String todayCode = now.getDayOfWeek().name().substring(0, 3); // MON, TUE, ...

        for (BookingTemplate template : templateRepository.findByActiveTrue()) {
            if (today.equals(template.getLastMaterializedDate())) continue;
            if (!matchesToday(template.getDaysOfWeek(), todayCode)) continue;
            if (template.getTimeOfDay() == null) continue;

            LocalDateTime scheduled = LocalDateTime.of(today, template.getTimeOfDay());
            LocalDateTime windowStart = scheduled.minusMinutes(LEAD_MINUTES);

            if (now.isBefore(windowStart)) continue; // too early

            // Stamp first so this template is attempted at most once today.
            template.setLastMaterializedDate(today);
            templateRepository.save(template);

            // If the whole window was missed (e.g. downtime), just skip — don't
            // create a retroactive booking.
            if (now.isAfter(scheduled)) continue;

            materializeOne(template);
        }
    }

    private void materializeOne(BookingTemplate template) {
        try {
            BookingRequest request = new BookingRequest();
            request.setUserId(template.getUserId());
            request.setStationId(template.getStationId());
            request.setConnectorType(ConnectorType.valueOf(template.getConnectorType()));
            request.setVehicleType(VehicleType.valueOf(template.getVehicleType()));
            request.setAllowTruckSlotFallback(true); // auto-pick any usable slot

            bookingService.createBooking(request);

            pushNotificationService.sendToUser(
                    template.getUserId(),
                    "BOOKING_EXPIRING",
                    "Recurring slot booked",
                    "Your recurring slot is reserved. Start charging within 20 minutes to keep it.",
                    "plugsy://bookings/" + template.getUserId());
        } catch (Exception e) {
            log.info("Recurring booking failed for template {}: {}", template.getId(), e.getMessage());
            pushNotificationService.sendToUser(
                    template.getUserId(),
                    "BOOKING_EXPIRING",
                    "Recurring booking unavailable",
                    "We couldn't book your recurring slot today — no matching connector was free.",
                    "plugsy://bookings/" + template.getUserId());
        }
    }

    private boolean matchesToday(String daysOfWeek, String todayCode) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) return false;
        List<String> days = Arrays.stream(daysOfWeek.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
        return days.contains(todayCode);
    }
}
