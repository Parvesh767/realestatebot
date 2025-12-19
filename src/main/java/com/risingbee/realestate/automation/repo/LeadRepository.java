package com.risingbee.realestate.automation.repo;

import java.time.Instant;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.risingbee.realestate.automation.domain.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {

	List<Lead> findByBrokerIdOrderByCreatedAtDesc(Long brokerId);

	java.util.Optional<Lead> findTopByPhoneNumberOrderByCreatedAtDesc(String phone);
	
	@Query("""
		    SELECT COUNT(l)
		    FROM Lead l
		    WHERE l.broker.id = :brokerId
		      AND l.createdAt >= :startOfMonth
		""")
	static
		long countMonthlyLeads(
		    @Param("brokerId") Long brokerId,
		    @Param("startOfMonth") Instant startOfMonth
		) {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
