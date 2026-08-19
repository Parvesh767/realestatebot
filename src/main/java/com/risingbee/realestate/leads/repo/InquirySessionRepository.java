package com.risingbee.realestate.leads.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.risingbee.realestate.leads.domain.InquirySession;

@Repository
public interface InquirySessionRepository extends JpaRepository<InquirySession, Long> {

    /**
     * Find the most recent active/unconverted inquiry session for a given searcher phone number.
     */
    Optional<InquirySession> findTopBySearcherPhoneAndConvertedFalseOrderByCreatedAtDesc(String searcherPhone);
}