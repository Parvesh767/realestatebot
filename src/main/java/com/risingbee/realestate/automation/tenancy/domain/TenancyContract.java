package com.risingbee.realestate.automation.tenancy.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenancy_contracts")
@Getter
@Setter
@NoArgsConstructor
public class TenancyContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column
    String tenantName;
    
    
    @Column(nullable = false)
    private Long propertyId;

    @Column(nullable = false)
    private Long ownerAccountId;

    @Column(nullable = false)
    private Long tenantAccountId;

    private String tenantPhone;
    private String ownerPhone;

    @Column(nullable = false)
    private Long monthlyRent;

    @Column(nullable = false)
    private Long securityDeposit; // Initial deposit paid

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate actualMoveOutDate;

    private Integer noticePeriodDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenancyStatus status = TenancyStatus.ACTIVE;

    @OneToMany(mappedBy = "tenancyContract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InspectionRecord> inspections = new ArrayList<>();

    @OneToMany(mappedBy = "tenancyContract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepositDeduction> deductions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public void addInspection(InspectionRecord inspection) {
        inspections.add(inspection);
        inspection.setTenancyContract(this);
    }

    public void addDeduction(DepositDeduction deduction) {
        deductions.add(deduction);
        deduction.setTenancyContract(this);
    }
}