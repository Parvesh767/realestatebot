package com.risingbee.realestate.automation.utility.dto;

import lombok.Data;

@Data
public class CreateUtilityBillRequestDTO {
    private String billingMonth;
    private Double previousReading;
    private Double currentReading;
    private Double ratePerUnit = 8.5;
    private Long fixedMaintenance = 0L;
    private Long fixedWater = 0L;
}