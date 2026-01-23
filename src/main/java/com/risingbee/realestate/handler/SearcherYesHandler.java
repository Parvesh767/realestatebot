package com.risingbee.realestate.handler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.repo.LeadRepository;
import com.risingbee.realestate.automation.service.LeadService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.location.display.service.LocationDisplayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearcherYesHandler {

    private final LeadService leadService;
    private final LeadRepository leadRepository;
    private final WhatsAppSender whatsAppSender;
    private final LocationDisplayService lcoalDisplayService;

    private static final int FREE_LEAD_LIMIT = 10;
    private static final Duration YES_EXPIRY_WINDOW = Duration.ofMinutes(3);

    public void handle(String searcherPhone, String messageId) {

        Optional<Lead> latestLeadOpt =
            leadService.findLatestByPhone(searcherPhone);

        if (latestLeadOpt.isEmpty()) {
            send(searcherPhone, "I couldn’t find a recent enquiry. Please search again.");
            return;
        }

        Lead lead = latestLeadOpt.get();

        if (!lead.markYesProcessed(messageId)) {
            log.info("Duplicate YES ignored. messageId={}", messageId);
            return;
        }
        leadRepository.save(lead);

        if (isExpired(lead)) {
            send(
                searcherPhone,
                """
                ⏳ This enquiry has expired.

                Please search again.
                """
            );
            return;
        }

        Long ownerAccountId = lead.getOwnerAccountId();
        if (ownerAccountId == null) {
            log.error("Lead {} has no ownerAccountId", lead.getId());
            return;
        }

        Instant startOfMonth =
            LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        long usedLeads =
            leadRepository.countMonthlyLeads(
                ownerAccountId,
                startOfMonth
            );

        if (usedLeads >= FREE_LEAD_LIMIT) {

            notifyOwnerLimitReached(searcherPhone, lead);

            send(
                searcherPhone,
                """
                🚫 The listing owner has reached the free enquiry limit.

                Please try again later.
                """
            );
            return;
        }

        notifyOwnerNewLead(searcherPhone, lead);

        send(
            searcherPhone,
            "✅ Thanks! The owner has been notified and will contact you shortly."
        );
    }

    /* ================= Notifications ================= */

    private void notifyOwnerNewLead(String ownerPhone, Lead lead)
 {

        String msg = """
            📢 New Interested Lead

            📞 Phone: %s
            🏠 Requirement: %s
            📍 Location: %s
            💰 Budget: %s
            """.formatted(
                lead.getPhoneNumber(),
                lead.getBhk(),
                displayLocation(lead),
                formatBudget(lead)
            );

        send(ownerPhone, msg);
    }

    private void notifyOwnerLimitReached(String ownerPhone, Lead lead) {

        String msg = """
            ⚠️ Lead Blocked (Limit Reached)

            A user tried to contact you:

            🏠 %s
            📍 %s
            💰 %s

            Upgrade your plan to continue
            receiving enquiries.
            """.formatted(
                lead.getBhk(),
                displayLocation(lead),
                formatBudget(lead)
            );

        send(ownerPhone, msg);
    }

    /* ================= Helpers ================= */

    private boolean isExpired(Lead lead) {
        return Instant.now()
            .isAfter(
                lead.getCreatedAt().plus(YES_EXPIRY_WINDOW)
            );
    }

    private String displayLocation(Lead lead) {
        if (lead.getLocalityCode() != null) {
        	return lcoalDisplayService.localityName(lead.getLocalityCode());
        }
        if (lead.getCityCode() != null) {
            return lead.getCityCode();
        }
        return "Not specified";
    }

    private String formatBudget(Lead lead) {
        Integer min = lead.getMinBudget();
        Integer max = lead.getMaxBudget();

        if (min == null && max == null) return "Not specified";
        if (min != null && min.equals(max)) {
            return "₹" + formatAmount(min);
        }
        if (min != null && max != null) {
            return "₹" + formatAmount(min) + " – ₹" + formatAmount(max);
        }
        if (min != null) return "From ₹" + formatAmount(min);
        return "Up to ₹" + formatAmount(max);
    }

    private String formatAmount(Integer amount) {
        return String.format("%,d", amount);
    }

    private void send(String phone, String message) {
        whatsAppSender.sendTextMessage(phone, message);
    }
}
