package com.risingbee.realestate.automation.tenancy.dto;

import com.risingbee.realestate.automation.tenancy.enums.DeductionCategory;
import lombok.Data;

@Data
public class DeductionRequestDTO {
    private DeductionCategory category;
    private String title;
    private Long amount;
}