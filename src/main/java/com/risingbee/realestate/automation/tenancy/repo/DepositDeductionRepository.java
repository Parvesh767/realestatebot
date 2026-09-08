package com.risingbee.realestate.automation.tenancy.repo;

import com.risingbee.realestate.automation.tenancy.domain.DepositDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositDeductionRepository extends JpaRepository<DepositDeduction, Long> {
    List<DepositDeduction> findByTenancyContractId(Long tenancyContractId);
}