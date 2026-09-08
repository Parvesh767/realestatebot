package com.risingbee.realestate.automation.service_hub.domain;

import java.time.Instant;

import com.risingbee.realestate.automation.service_hub.enums.ServiceCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_vendors")
@Getter
@Setter
@NoArgsConstructor
public class ServiceVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory skillCategory;

    private String operationalCityCode;
    private String operationalLocalityCode;

    private Double rating = 5.0;
    private Integer completedJobsCount = 0;

    private boolean active = true;
    private Instant createdAt = Instant.now();
    
    private String upiId; // e.g. "ramesh@oksbi"
    private Long pendingPayoutBalance = 0L; // Accrued earnings ready for batch payout
    private Long totalPaidOut = 0L;
    
    
}