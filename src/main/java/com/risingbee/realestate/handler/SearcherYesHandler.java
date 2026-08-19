package com.risingbee.realestate.handler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.repo.LeadRepository;
import com.risingbee.realestate.automation.service.LeadService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.leads.domain.InquirySession;
import com.risingbee.realestate.leads.repo.InquirySessionRepository;
import com.risingbee.realestate.leads.service.InquirySessionService;
import com.risingbee.realestate.location.display.service.LocationDisplayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearcherYesHandler {

    private final LeadService leadService;
    private final LeadRepository leadRepository;
    private final AccountService accountService;
    private final WhatsAppSender whatsAppSender;
    private final LocationDisplayService lcoalDisplayService;
    private final InquirySessionService inquirySessionService;
    private final InquirySessionRepository inquirySessionRepository;

    private static final int FREE_LEAD_LIMIT = 10;
    private static final Duration YES_EXPIRY_WINDOW = Duration.ofMinutes(15);

    public void handle(String searcherPhone, String messageId) {
        Optional<InquirySession> inquiryOpt = inquirySessionService.findLatestOpen(searcherPhone);

        if (inquiryOpt.isEmpty()) {
            send(searcherPhone, "I couldn’t find a recent enquiry. Please search for a property again!");
            return;
        }

        InquirySession inquiry = inquiryOpt.get();

     // 1. Get Optional<Lead>
     Optional<Lead> leadOpt = leadService.createQualifiedLead(inquiry);

     if (leadOpt.isEmpty()) {
         log.info("Duplicate inquiry detected or lead creation suppressed for session={}", inquiry.getId());
         send(searcherPhone, "✅ We've already recorded your request. The property owner will contact you shortly!");
         return;
     }

     Lead lead = leadOpt.get();

     // 2. Mark YES message processed
     if (!lead.markYesProcessed(messageId)) {
         log.info("Duplicate YES ignored for messageId={}", messageId);
         return;
     }
     leadRepository.save(lead);

     inquiry.markConverted();
     inquirySessionRepository.save(inquiry);

        if (isExpired(lead)) {
            send(searcherPhone, """
                    ⏳ This enquiry session has expired.

                    Please search for a property again.
                    """);
            return;
        }

        Long ownerAccountId = lead.getOwnerAccountId();
        if (ownerAccountId == null) {
            log.error("Lead {} has no ownerAccountId", lead.getId());
            send(searcherPhone, "✅ Thanks! Your inquiry has been registered.");
            return;
        }

        Optional<Account> ownerAccountOpt = accountService.findById(ownerAccountId);

        if (ownerAccountOpt.isEmpty() || !isValidPhoneNumber(ownerAccountOpt.get().getExternalId())) {
            log.warn("Invalid or missing owner phone for accountId={}. Skipping broker WhatsApp alert.", ownerAccountId);
            send(searcherPhone, "✅ Thanks! Your request has been registered with our team.");
            return;
        }

        String ownerPhone = ownerAccountOpt.get().getExternalId();
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long usedLeads = leadRepository.countMonthlyLeads(ownerAccountId, startOfMonth);

        if (usedLeads >= FREE_LEAD_LIMIT) {
            notifyOwnerLimitReached(ownerPhone, lead);
            send(searcherPhone, """
                    🚫 The listing owner has reached their monthly lead limit.

                    Please try searching for another property in the area!
                    """);
            return;
        }

        notifyOwnerNewLead(ownerPhone, lead);
        send(searcherPhone, "✅ Thanks! The property owner has been notified and will contact you shortly.");
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^[1-9][0-9]{9,14}$") && !phone.contains("0000000000");
    }

    private void notifyOwnerNewLead(String ownerPhone, Lead lead) {
        String msg = """
                📢 *New Interested Lead Alert!*

                📞 *Buyer Phone:* %s
                🛏 *Requirement:* %s
                📍 *Location:* %s
                💰 *Budget:* %s
                """.formatted(lead.getPhoneNumber(), lead.getBhk() != null ? lead.getBhk() : "Any BHK",
                displayLocation(lead), formatBudget(lead));

        send(ownerPhone, msg);
    }

    private void notifyOwnerLimitReached(String ownerPhone, Lead lead) {
        String msg = """
                ⚠️ *Lead Blocked (Free Limit Reached)*

                A buyer tried to contact you for your listing:
                🏠 Requirement: %s
                📍 Location: %s
                💰 Budget: %s

                ⚡ Upgrade your subscription or recharge credits to receive buyer details!
                """.formatted(lead.getBhk() != null ? lead.getBhk() : "Property", displayLocation(lead),
                formatBudget(lead));

        send(ownerPhone, msg);
    }

    private boolean isExpired(Lead lead) {
        return Instant.now().isAfter(lead.getCreatedAt().plus(YES_EXPIRY_WINDOW));
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
        Long min = lead.getMinBudget();
        Long max = lead.getMaxBudget();

        if (min == null && max == null) return "Not specified";
        if (min != null && min.equals(max)) return "₹" + formatAmount(min);
        if (min != null && max != null) return "₹" + formatAmount(min) + " – ₹" + formatAmount(max);
        if (min != null) return "From ₹" + formatAmount(min);
        return "Up to ₹" + formatAmount(max);
    }

    private String formatAmount(Number amount) {
        if (amount == null) return "N/A";
        return String.format("%,d", amount.longValue());
    }

    private void send(String phone, String message) {
        whatsAppSender.sendTextMessage(phone, message);
    }
}