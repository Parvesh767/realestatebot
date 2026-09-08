package com.risingbee.realestate.automation.utility.domain;

import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "utility_bills")
@Getter
@Setter
@NoArgsConstructor
public class UtilityBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenancy_contract_id", nullable = false)
    private TenancyContract contract;

    private String billingMonth; // e.g., "August 2026"
    private LocalDate readingDate = LocalDate.now();

    private Double previousMeterReading;
    private Double currentMeterReading;
    private Double unitsConsumed; // current - previous

    private Double ratePerUnit = 8.5; // ₹8.5 / Unit (Standard residential sub-meter slab)
    private Long electricityAmountInr;
    private Long maintenanceChargeInr = 0L;
    private Long waterChargeInr = 0L;
    private Long totalPayableInr;

    private String meterPhotoUrl;
    private boolean paid = false;
    private Instant paidAt;
    private Instant createdAt = Instant.now();
}