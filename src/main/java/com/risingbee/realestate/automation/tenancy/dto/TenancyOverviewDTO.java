package com.risingbee.realestate.automation.tenancy.dto;

import com.risingbee.realestate.automation.rent.domain.RentPaymentStatus;
import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancyOverviewDTO {
    private Long contractId;
    private Long propertyId;
    private String propertyTitle;
    private String bhk;
    private String locality;
    private String tenantName;
    private String tenantPhone;
    private Long monthlyRent;
    private Long securityDeposit;
    private LocalDate startDate;
    private LocalDate endDate;
    private TenancyStatus tenancyStatus;
    
    // Rent health for current cycle
    private String currentMonthCycle;
    private Long currentTotalDue;
    private RentPaymentStatus currentRentStatus;
    private Long totalArrears;
}