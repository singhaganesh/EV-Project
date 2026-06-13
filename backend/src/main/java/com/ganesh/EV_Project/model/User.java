package com.ganesh.EV_Project.model;

import com.ganesh.EV_Project.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", unique = true)
    private String mobileNumber;

    @Column(nullable = true, unique = true)
    private String email;

    private String name;

    private String password;

    @Column(name = "is_first_time_user")
    private Boolean isFirstTimeUser = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    // Account lifecycle. Defaults to APPROVED so existing customer/admin flows are
    // unaffected; owner registration overrides this to PENDING_EMAIL_VERIFICATION.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private UserStatus status = UserStatus.APPROVED;

    // Email-based MFA (owners only). Off by default.
    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 100)
    private String mfaSecret;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Role {
        CUSTOMER,
        STATION_OWNER,
        ADMIN
    }
}
