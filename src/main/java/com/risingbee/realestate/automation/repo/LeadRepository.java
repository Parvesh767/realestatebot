package com.risingbee.realestate.automation.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.automation.domain.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {

	List<Lead> findByBrokerIdOrderByCreatedAtDesc(Long brokerId);
}
