package com.risingbee.realestate.automation.rent.repo;

import com.risingbee.realestate.automation.rent.domain.RentPaymentStatus;
import com.risingbee.realestate.automation.rent.domain.RentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentScheduleRepository extends JpaRepository<RentSchedule, Long> {

    List<RentSchedule> findByContractIdOrderByDueDateDesc(Long contractId);

    Optional<RentSchedule> findByContractIdAndBillingCycleMonth(Long contractId, String billingCycleMonth);

    List<RentSchedule> findByStatusAndDueDateBefore(RentPaymentStatus status, LocalDate cutoffDate);

    @Query("SELECT r FROM RentSchedule r WHERE r.status IN ('PENDING', 'OVERDUE') AND r.dueDate <= :targetDate")
    List<RentSchedule> findPendingRentsForReminder(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT COALESCE(SUM(r.totalDueInr - r.paidAmountInr), 0) FROM RentSchedule r WHERE r.contract.id = :contractId AND r.status != 'PAID'")
    Long getPendingArrearsByContract(@Param("contractId") Long contractId);
}