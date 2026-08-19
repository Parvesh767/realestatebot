package com.risingbee.realestate.automation.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.auth.AuthorizationService;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.repo.LeadRepository;
import com.risingbee.realestate.leads.domain.InquirySession;
import com.risingbee.realestate.leads.enums.ActivityType;
import com.risingbee.realestate.leads.service.LeadActivityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LeadService {

    private final LeadRepository leadRepository;
    private final AuthorizationService authz;
    private final LeadActivityService leadActivityService;

    // Cooldown window to prevent duplicate lead alerts to the same broker
    private static final int DEDUPLICATION_WINDOW_MINUTES = 10;

    /**
     * Creates a lead if one does not already exist within the deduplication window.
     * Returns Optional.empty() if duplicate, signaling to skip WhatsApp alert.
     */
    public Optional<Lead> createFromMatchedProperty(
        String phone,
        Property property,
        String rawMessage,
        String cityCode,
        String localityCode
    ) {
        Long ownerAccountId = property.getOwnerAccountId();
        if (ownerAccountId == null) {
            return Optional.empty();
        }

        // 1. Check if an identical lead already exists within the last 10 minutes
        Instant cooldownWindow = Instant.now().minus(DEDUPLICATION_WINDOW_MINUTES, ChronoUnit.MINUTES);
        Optional<Lead> existingLead = leadRepository
                .findTopByOwnerAccountIdAndPhoneNumberAndCreatedAtAfterOrderByCreatedAtDesc(
                        ownerAccountId, phone, cooldownWindow);

        if (existingLead.isPresent()) {
            log.info("Duplicate search inquiry detected for ownerAccountId={} from phone={}. Skipping WhatsApp alert.",
                    ownerAccountId, phone);
            return Optional.empty(); // 🛑 Returns empty so caller does NOT send duplicate WhatsApp messages
        }

        // 2. Persist new Lead
        Lead lead = new Lead(
            phone,
            ownerAccountId,
            property.getBhk(),
            property.getPrice(),
            property.getPrice(),
            cityCode,
            localityCode,
            rawMessage
        );
        
        Lead savedLead = leadRepository.save(lead);

        leadActivityService.log(
            savedLead.getId(),
            ownerAccountId,
            ActivityType.AUTO_CREATED,
            "Lead created from WhatsApp inquiry"
        );

        log.info("Created NEW Lead id={} for ownerAccountId={}", savedLead.getId(), ownerAccountId);
        return Optional.of(savedLead);
    }

    public Optional<Lead> createLead(Actor buyer, Property property, ParsedRequest parsed) {
        return createFromMatchedProperty(
            buyer.externalId(),
            property,
            parsed.msg(),
            property.getCityCode(),
            property.getLocalityCode()
        );
    }

    public Optional<Lead> createQualifiedLead(InquirySession inquiry) {
        Instant cooldownWindow = Instant.now().minus(DEDUPLICATION_WINDOW_MINUTES, ChronoUnit.MINUTES);
        Optional<Lead> existingLead = leadRepository
                .findTopByOwnerAccountIdAndPhoneNumberAndCreatedAtAfterOrderByCreatedAtDesc(
                        inquiry.getOwnerAccountId(), inquiry.getSearcherPhone(), cooldownWindow);

        if (existingLead.isPresent()) {
            log.info("Duplicate qualified inquiry detected for ownerAccountId={} from phone={}. Skipping alert.",
                    inquiry.getOwnerAccountId(), inquiry.getSearcherPhone());
            return Optional.empty();
        }

        Lead lead = new Lead(
            inquiry.getSearcherPhone(),
            inquiry.getOwnerAccountId(),
            inquiry.getBhk(),
            inquiry.getMinBudget(),
            inquiry.getMaxBudget(),
            inquiry.getCityCode(),
            inquiry.getLocalityCode(),
            inquiry.getRawMessage()
        );

        Lead saved = leadRepository.save(lead);
        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public Optional<Lead> findLatestLockedLeadForOwner(Long ownerAccountId) {
        if (ownerAccountId == null) {
            return Optional.empty();
        }
        return leadRepository.findTopByOwnerAccountIdAndUnlockedFalseOrderByCreatedAtDesc(ownerAccountId);
    }

    @Transactional(readOnly = true)
    public List<Lead> findAllForActor(Actor actor) {
        authz.require(actor, Capability.ADD_PROPERTY);

        Long accountId = actor.internalId();
        if (accountId == null) {
            throw new IllegalStateException("Actor has no accountId");
        }

        return leadRepository.findByOwnerAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public Optional<Lead> findLatestByPhone(String phone) {
        return leadRepository.findTopByPhoneNumberOrderByCreatedAtDesc(phone);
    }
}