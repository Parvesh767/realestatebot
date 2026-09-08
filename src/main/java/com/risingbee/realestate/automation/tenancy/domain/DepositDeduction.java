package com.risingbee.realestate.automation.tenancy.domain;

import com.risingbee.realestate.automation.tenancy.enums.DeductionCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "deposit_deductions")
@Getter
@Setter
@NoArgsConstructor
public class DepositDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenancy_contract_id", nullable = false)
    private TenancyContract tenancyContract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionCategory category;

    @Column(nullable = false)
    private String title; // e.g. "Living Room Wall Repaint & Patch"

    @Column(nullable = false)
    private Long amount;

    private String proofPhotoUrl; // Photo of damage / unpaid electricity bill

    private Long relatedServiceBookingId; // Optional link to platform service booking

    @Column(nullable = false)
    private boolean approvedByTenant = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}