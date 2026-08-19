package com.risingbee.realestate.leads.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.leads.domain.InquirySession;
import com.risingbee.realestate.leads.repo.InquirySessionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InquirySessionService {

    private final InquirySessionRepository inquirySessionRepository;

    /**
     * Creates an InquirySession linked to a matched Property
     */
    public InquirySession create(
        String searcherPhone,
        String rawMessage,
        Property property,
        String cityCode,
        String localityCode
    ) {
        Long price = property.getPrice();

        InquirySession inquiry = new InquirySession(
            searcherPhone,
            rawMessage,
            property.getBhk(),
            price,
            price,
            cityCode,
            localityCode,
            property.getId(),
            property.getOwnerAccountId()
        );

        return inquirySessionRepository.save(inquiry);
    }

    /**
     * 🔥 FIX: Fetch the latest unconverted inquiry session for buyer YES confirmations
     */
    @Transactional(readOnly = true)
    public Optional<InquirySession> findLatestOpen(String searcherPhone) {
        if (searcherPhone == null || searcherPhone.isBlank()) {
            return Optional.empty();
        }
        return inquirySessionRepository.findTopBySearcherPhoneAndConvertedFalseOrderByCreatedAtDesc(searcherPhone);
    }
}