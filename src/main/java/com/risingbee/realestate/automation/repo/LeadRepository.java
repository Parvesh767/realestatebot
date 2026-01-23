package com.risingbee.realestate.automation.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.risingbee.realestate.automation.domain.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {
	
	List<Lead> findByOwnerAccountIdOrderByCreatedAtDesc(Long ownerAccountId);


    @Query("""
        SELECT COUNT(l)
        FROM Lead l
        WHERE l.ownerAccountId = :ownerAccountId
          AND l.createdAt >= :startOfMonth
    """)
    long countMonthlyLeads(
        @Param("ownerAccountId") Long ownerAccountId,
        @Param("startOfMonth") Instant startOfMonth
    );   

	Optional<Lead> findTopByPhoneNumberOrderByCreatedAtDesc(String phone);
	
	
	
}
