package com.risingbee.realestate.leads.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.leads.domain.LeadActivity;


public interface LeadActivityRepository
    extends JpaRepository<LeadActivity, Long> {

    List<LeadActivity>
    findByLeadIdOrderByCreatedAtDesc(Long leadId);
    

//    List<LeadActivity> findByOwnerAccountIdOrderByCreatedAtDesc(Long accountId);
    
    
    

    // Matching private Long accountId in LeadActivity
    List<LeadActivity> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    
    
}