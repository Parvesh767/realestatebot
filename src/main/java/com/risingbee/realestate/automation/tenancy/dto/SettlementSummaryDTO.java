package com.risingbee.realestate.automation.tenancy.dto;

import com.risingbee.realestate.automation.tenancy.domain.DepositDeduction;
import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSummaryDTO {
    private Long contractId;
    private Long propertyId;
    private Long originalDeposit;
    private Long totalDeductions;
    private Long netRefundableAmount;
    private TenancyStatus status;
    private List<DepositDeduction> deductionItems;
    private Long unpaidRentArrears;
}