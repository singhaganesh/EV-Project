package com.ganesh.EV_Project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Business/KYC metadata for a station owner. One-to-one with {@link User}.
 * Document path columns hold the Supabase Storage object path for each uploaded file.
 */
@Entity
@Table(name = "business_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "company_name", length = 150, nullable = false)
    private String companyName;

    @Column(name = "tax_id", length = 50, nullable = false)
    private String taxId;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "bank_account_number", length = 50, nullable = false)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 30, nullable = false)
    private String bankIfscCode;

    @Column(name = "registration_doc_path")
    private String registrationDocPath;

    @Column(name = "electricity_doc_path")
    private String electricityDocPath;

    @Column(name = "bank_doc_path")
    private String bankDocPath;
}
