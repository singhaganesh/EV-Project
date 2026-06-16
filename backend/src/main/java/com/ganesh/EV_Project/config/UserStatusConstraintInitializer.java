package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Keeps the Postgres {@code users_status_check} constraint in sync with the
 * {@link UserStatus} enum.
 *
 * Hibernate 6 generates a CHECK constraint for {@code @Enumerated(STRING)}
 * columns when the table is first created, but {@code ddl-auto=update} never
 * alters it afterwards — so a newly added enum value (e.g. DELETED) is rejected
 * by the database. This runner rebuilds the constraint from the current enum on
 * startup, idempotently, so adding enum values "just works". Safe: the rebuilt
 * constraint is always a superset, so no existing row can be invalidated.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserStatusConstraintInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void syncConstraint() {
        try {
            String values = Arrays.stream(UserStatus.values())
                    .map(v -> "'" + v.name() + "'")
                    .collect(Collectors.joining(", "));
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check");
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD CONSTRAINT users_status_check CHECK (status IN (" + values + "))");
            log.info("Synced users_status_check with UserStatus enum ({})", values);
        } catch (Exception e) {
            // Non-fatal: only affects writing newly added status values.
            log.warn("Could not sync users_status_check constraint: {}", e.getMessage());
        }
    }
}
