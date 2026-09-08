package com.risingbee.realestate.automation.rent.domain;

import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "rent_schedules")
@Getter
@Setter
@NoArgsConstructor
public class RentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenancy_contract_id", nullable = true)
    private TenancyContract contract;

    @Column(nullable = false)
    private String billingCycleMonth; // e.g. "September 2026"

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private Long baseRentInr;

    private Long electricityInr = 0L;
    private Long maintenanceInr = 0L;
    private Long waterInr = 0L;
    private Long lateFeeInr = 0L;
    
    @Column(name = "receipt_photo_url")
    private String receiptPhotoUrl;


    @Column(nullable = false)
    private Long totalDueInr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentPaymentStatus status = RentPaymentStatus.PENDING;

    private Long paidAmountInr = 0L;
    private Instant paidAt;
    private String paymentMode;
    private String transactionReference;

    private Integer reminderCount = 0;
    private Instant lastReminderSentAt;

    private Instant createdAt = Instant.now();
}
