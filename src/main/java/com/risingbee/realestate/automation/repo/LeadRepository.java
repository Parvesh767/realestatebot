package com.risingbee.realestate.automation.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.leads.enums.LeadStatus;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    // Fetch all leads for owner ordered by newest first
    List<Lead> findByOwnerAccountIdOrderByCreatedAtDesc(Long ownerAccountId);

    // Fetch by owner and unlock status (For Unlocked vs. Masked tabs)
    List<Lead> findByOwnerAccountIdAndUnlockedOrderByCreatedAtDesc(Long ownerAccountId, Boolean unlocked);

    // Rate limiting: Count leads in current billing cycle
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

    List<Lead> findByOwnerAccountIdAndStatusOrderByCreatedAtDesc(
        Long ownerAccountId,
        LeadStatus status
    );

    @Query("""
        SELECT l
        FROM Lead l
        WHERE l.ownerAccountId = :ownerAccountId
          AND l.nextFollowUpAt <= :now
          AND l.status NOT IN ('CLOSED', 'LOST')
    """)
    List<Lead> findOverdueLeads(
        @Param("ownerAccountId") Long ownerAccountId,
        @Param("now") Instant now
    );
    
    
    Optional<Lead> findTopByOwnerAccountIdAndPhoneNumberAndCreatedAtAfterOrderByCreatedAtDesc(
    	    Long ownerAccountId, 
    	    String phoneNumber, 
    	    Instant afterTime
    	);

    /**
     * Fetches the most recent locked lead for a specific broker account.
     */
    Optional<Lead> findTopByOwnerAccountIdAndUnlockedFalseOrderByCreatedAtDesc(Long ownerAccountId);
}