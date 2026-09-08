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
import com.risingbee.realestate.leads.enums.LeadSource;
import com.risingbee.realestate.leads.enums.LeadStatus;
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

    private static final int DEDUPLICATION_WINDOW_MINUTES = 10;

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

        Instant cooldownWindow = Instant.now().minus(DEDUPLICATION_WINDOW_MINUTES, ChronoUnit.MINUTES);
        Optional<Lead> existingLead = leadRepository
                .findTopByOwnerAccountIdAndPhoneNumberAndCreatedAtAfterOrderByCreatedAtDesc(
                        ownerAccountId, phone, cooldownWindow);

        if (existingLead.isPresent()) {
            log.info("Duplicate search inquiry detected for ownerAccountId={} from phone={}. Skipping WhatsApp alert.",
                    ownerAccountId, phone);
            return Optional.empty();
        }

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
        
        lead.setSource(LeadSource.WHATSAPP_AUTOMATION);
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

        lead.setSource(LeadSource.WHATSAPP_AUTOMATION);
        Lead saved = leadRepository.save(lead);

        // Added missing activity log for qualified WhatsApp leads
        leadActivityService.log(
            saved.getId(),
            inquiry.getOwnerAccountId(),
            ActivityType.AUTO_CREATED,
            "Lead created from qualified WhatsApp session"
        );

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

    public Lead updateLeadStatus(Long leadId, LeadStatus status) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found with id: " + leadId));
        
        lead.setStatus(status);
        lead.setUpdatedAt(Instant.now());
        Lead saved = leadRepository.save(lead);

        // Record status change event to timeline history
        leadActivityService.log(
            leadId,
            lead.getOwnerAccountId(),
            ActivityType.AUTO_CREATED, // Or custom activity type if available
            "Lead status updated to " + status
        );

        return saved;
    }
}