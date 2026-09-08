package com.risingbee.realestate.automation.tenancy.repo;

import com.risingbee.realestate.automation.tenancy.domain.InspectionRecord;
import com.risingbee.realestate.automation.tenancy.enums.InspectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long> {

    /**
     * Retrieve all inspection audits (move-in, move-out, routine) for a tenancy agreement.
     */
    List<InspectionRecord> findByTenancyContractIdOrderByCreatedAtAsc(Long tenancyContractId);

    /**
     * Fetch the baseline Move-In inspection audit for a contract.
     */
    Optional<InspectionRecord> findByTenancyContractIdAndType(Long tenancyContractId, InspectionType type);

    /**
     * Query inspections recorded by a specific user/broker.
     */
    List<InspectionRecord> findByRecordedByAccountIdOrderByCreatedAtDesc(Long recordedByAccountId);

    /**
     * Fetch the most recent inspection for comparison during move-out settlement.
     */
    @Query("SELECT r FROM InspectionRecord r WHERE r.tenancyContract.id = :contractId ORDER BY r.createdAt DESC LIMIT 1")
    Optional<InspectionRecord> findLatestInspectionForContract(@Param("contractId") Long contractId);
}