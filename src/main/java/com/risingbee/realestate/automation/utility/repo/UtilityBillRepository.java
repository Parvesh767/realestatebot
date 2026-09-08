package com.risingbee.realestate.automation.utility.repo;

import com.risingbee.realestate.automation.utility.domain.UtilityBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityBillRepository extends JpaRepository<UtilityBill, Long> {

    /**
     * Retrieve all utility bills for a given contract ordered by newest first.
     */
    List<UtilityBill> findByContractIdOrderByReadingDateDesc(Long contractId);

    /**
     * Retrieve the most recent bill for a tenancy contract to determine the baseline previous reading.
     */
    Optional<UtilityBill> findFirstByContractIdOrderByReadingDateDesc(Long contractId);

    /**
     * Check if a bill for a specific month (e.g., "August 2026") has already been generated.
     */
    Optional<UtilityBill> findByContractIdAndBillingMonth(Long contractId, String billingMonth);

    /**
     * Fetch all unpaid bills across a tenancy contract (useful for Move-Out Settlement audit).
     */
    List<UtilityBill> findByContractIdAndPaidFalse(Long contractId);

    /**
     * Sum of total unpaid utility balance across a contract.
     */
    @Query("SELECT COALESCE(SUM(u.totalPayableInr), 0) FROM UtilityBill u WHERE u.contract.id = :contractId AND u.paid = false")
    Long sumUnpaidAmountByContractId(@Param("contractId") Long contractId);
}