package com.risingbee.realestate.automation.lead.service;

import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.repo.LeadRepository;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.leads.enums.LeadSource;
import com.risingbee.realestate.leads.enums.LeadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LeadIngestionService {

    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final AccountRepository accountRepository;
    private final WhatsAppSender whatsAppSender;

    public void processWebInquiry(Long propertyId, String tenantPhone, Integer moveInDays, LocalDate visitDate) {
        String cleanPhone = tenantPhone.replaceAll("[^0-9]", "");

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyId));

        // Find or associate tenant account ID for reputation score integration
        Long tenantAccountId = accountRepository.findByExternalId(cleanPhone)
                .map(Account::getId)
                .orElse(null);

        // 1. Create a Masked Lead Record in the database
        Lead lead = new Lead();
        lead.setPhoneNumber(cleanPhone);
        lead.setPropertyId(propertyId);
        lead.setTenantAccountId(tenantAccountId);
        lead.setOwnerAccountId(property.getOwnerAccountId());
        lead.setBhk(property.getBhk());
        lead.setMinBudget(property.getPrice());
        lead.setMaxBudget(property.getPrice());
        lead.setCityCode(property.getCityCode());
        lead.setLocalityCode(property.getLocalityCode());
        lead.setUrgencyDays(moveInDays);
        lead.setPreferredVisitDate(visitDate);
        lead.setSource(LeadSource.WEB);
        lead.setStatus(LeadStatus.NEW);
        lead.setRawMessage(String.format("Web inquiry for Property #%d (%s, %s). Urgency: %d days, Visit: %s",
                propertyId, property.getBhk(), property.getTitle(), moveInDays, visitDate != null ? visitDate : "Flexible"));
        lead.setUnlocked(false); // Locked by default (preserves broker credit monetization)

        leadRepository.save(lead);

        // 2. Dispatch Confirmation WhatsApp to Tenant
        String tenantMsg = String.format(
                "🏢 *Property Inquiry Received!*\n\n" +
                "• Property: *%s* (#%d)\n" +
                "• Rent: *₹%d/mo*\n" +
                "• Preferred Visit: %s\n\n" +
                "Our verified local partner will contact you shortly to coordinate physical inspection.",
                property.getTitle(), propertyId, property.getPrice(), visitDate != null ? visitDate : "Coordinating"
        );
        whatsAppSender.sendTextMessage(cleanPhone, tenantMsg);

        // 3. Dispatch Masked Lead Alert to Assigned Broker or Owner
        String targetAlertPhone = property.getAssignedBrokerPhone();
        if (targetAlertPhone == null || targetAlertPhone.isBlank()) {
            targetAlertPhone = accountRepository.findById(property.getOwnerAccountId())
                    .map(Account::getPhone)
                    .orElse(null);
        }

        if (targetAlertPhone != null && !targetAlertPhone.isBlank()) {
            String maskedPhone = "+" + cleanPhone.substring(0, Math.min(4, cleanPhone.length())) + "••••••";
            String brokerAlert = String.format(
                    "🎯 *New Verified Tenant Inquiry!*\n\n" +
                    "• Property: #%d (%s)\n" +
                    "• Tenant: %s (Masked)\n" +
                    "• Move-in Urgency: %d days\n" +
                    "• Requested Visit: %s\n\n" +
                    "Unlock full contact details in your Broker Command Center for 1 Credit:\n" +
                    "http://localhost:8080/dashboard.html",
                    propertyId, property.getTitle(), maskedPhone, moveInDays, visitDate != null ? visitDate : "Flexible"
            );
            whatsAppSender.sendTextMessage(targetAlertPhone, brokerAlert);
        }

        log.info("Inquiry successfully ingested for Property #{} from Phone: {}", propertyId, cleanPhone);
    }
}