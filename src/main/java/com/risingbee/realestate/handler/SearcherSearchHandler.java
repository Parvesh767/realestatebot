package com.risingbee.realestate.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.dto.SearchResultResponse;
import com.risingbee.realestate.automation.parser.LocationResolver;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.ResolvedLocation;
import com.risingbee.realestate.automation.service.LeadService;
import com.risingbee.realestate.automation.service.PropertyService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.leads.service.InquirySessionService;
import com.risingbee.realestate.location.display.service.LocationDisplayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearcherSearchHandler {

    private final PropertyService propertyService;
    private final WhatsAppSender whatsAppSender;
    private final LeadService leadService;
    private final AccountService accountService;
    private final LocationResolver locationResolver;
    private final LocationDisplayService lcoalDisplayService;
    private final InquirySessionService inquirySessionService;

    public void handle(Actor actor, ParsedRequest parsed) {

        String from = actor.externalId();

        // 1️⃣ Validation
        if (!parsed.hasSearchIntent()) {
            whatsAppSender.sendTextMessage(
                from,
                """
                Please send a valid requirement format (location, BHK, budget).
                
                *Example:*
                _2 BHK rent near Sohna Road in Gurgaon under 30k_
                """
            );
            return;
        }

        // 2️⃣ Resolve Location
        ResolvedLocation rl = locationResolver.resolve(parsed).orElse(null);

        if (rl == null || rl.city() == null) {
            whatsAppSender.sendTextMessage(from, "❌ Please mention a valid location (e.g., Gurgaon, Sohna Road).");
            return;
        }

        // 3️⃣ Perform Matches
        SearchResultResponse result = findFlexibleMatches(parsed, rl);

        if (result.getProperties().isEmpty()) {
            whatsAppSender.sendTextMessage(
                from,
                "❌ No matching properties found right now.\nWe saved your search and will notify you when a property becomes available!"
            );
            return;
        }

        // 4️⃣ Create Inquiry Session
        inquirySessionService.create(
            from,
            parsed.normalizedText(),
            result.getProperties().get(0),
            rl.city(),
            rl.locality()
        );

        // 5️⃣ Send Property Cards to Buyer
        sendResults(from, result.getProperties(), result.getTag());

        // 6️⃣ 🔥 Capture Leads & Send Broker WhatsApp Alerts
        processLeadCaptureAndBrokerAlerts(actor, parsed, result.getProperties());
    }

    private void sendResults(String phone, List<Property> properties, String tag) {

        StringBuilder msg = new StringBuilder();
        
        if (tag != null && !tag.isBlank()) {
            msg.append(tag).append("\n\n");
        }
        
        msg.append("🏠 *Top Matches for Your Requirement*\n\n");

        int count = 0;

        for (Property p : properties) {

            String locality = lcoalDisplayService.localityName(p.getLocalityCode());
            String city = lcoalDisplayService.cityName(p.getCityCode());

            msg.append(count + 1).append("️⃣ ")
               .append(p.getTitle() != null ? p.getTitle() : p.getBhk() + " Property")
               .append("\n")
               .append("📍 Locality: ").append(locality != null ? locality : "General Area").append("\n")
               .append("🏙 City: ").append(city != null ? city : "Location specified").append("\n")
               .append("💰 Price: ₹").append(formatAmount(p.getPrice()))
               .append("\n\n");

            if (++count == 5) break;
        }

        msg.append("✨ *Best matches based on your requirement*\n\n")
           .append("👉 Reply *YES* to get owner/broker contact instantly\n")
           .append("⚡ Limited listings – act fast!");

        whatsAppSender.sendTextMessage(phone, msg.toString());
    }

    private void processLeadCaptureAndBrokerAlerts(Actor buyer, ParsedRequest parsed, List<Property> properties) {
        // Track notified brokers to prevent duplicate messages within the same search query
        Set<Long> notifiedBrokers = new HashSet<>();

        for (Property property : properties) {
            Long ownerAccountId = property.getOwnerAccountId();
            if (ownerAccountId == null) continue;

            // 1. Persist/Deduplicate Lead in database
            Optional<Lead> leadOpt = leadService.createFromMatchedProperty(
                buyer.externalId(),
                property,
                parsed.msg(),
                property.getCityCode(),
                property.getLocalityCode()
            );

            // 2. ONLY send WhatsApp message if a NEW lead was created AND broker was not already notified
            if (leadOpt.isPresent() && notifiedBrokers.add(ownerAccountId)) {
                Account ownerAccount = accountService.findById(ownerAccountId).orElse(null);

                if (ownerAccount != null && isValidWhatsAppNumber(ownerAccount.getExternalId())) {
                    String maskedPhone = maskPhoneNumber(buyer.externalId());

                    String alertText = """
                        🔥 *NEW HIGH-INTENT LEAD ALERT!*
                        
                        A buyer is interested in a property matching your listing: *%s*
                        
                        📱 *Buyer Contact:* %s
                        🛏 *Requirement:* %s
                        💰 *Budget:* ₹%s
                        
                        ⚡ _Reply *UNLOCK* or log in to your dashboard to view full contact info!_
                        """.formatted(
                            property.getTitle() != null ? property.getTitle() : property.getBhk(),
                            maskedPhone,
                            parsed.bhk() != null ? parsed.bhk() : "Any BHK",
                            formatAmount(parsed.maxBudget())
                        );

                    whatsAppSender.sendTextMessage(ownerAccount.getExternalId(), alertText);
                    log.info("Dispatched single lead alert to broker phone={}", ownerAccount.getExternalId());
                } else {
                    log.warn("Skipping WhatsApp alert for accountId={} (dummy/invalid phone: {})", 
                        ownerAccountId, ownerAccount != null ? ownerAccount.getExternalId() : "null");
                }
            }
        }
    }

    private boolean isValidWhatsAppNumber(String phone) {
        return phone != null 
            && phone.matches("^[1-9][0-9]{9,14}$") 
            && !phone.contains("0000000000")
            && !phone.equals("910000000000");
    }

    /**
     * Helper to mask phone numbers for monetization paywalls
     */
    private String maskPhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.length() < 8) return "XXXXXXXXXX";
        int len = rawPhone.length();
        return rawPhone.substring(0, len - 6) + "XXXXXX";
    }

    private SearchResultResponse findFlexibleMatches(ParsedRequest parsed, ResolvedLocation rl) {

        List<Property> exact = propertyService.findMatchesForSearchers(
            parsed.bhk(),
            rl.city(),
            rl.locality(),
            parsed.minBudget(),
            parsed.maxBudget()
        );

        if (!exact.isEmpty()) {
            return new SearchResultResponse(exact, null);
        }

        List<Property> cityOnly = propertyService.findMatchesForSearchers(
            parsed.bhk(),
            rl.city(),
            null,
            parsed.minBudget(),
            parsed.maxBudget()
        );

        if (!cityOnly.isEmpty()) {
            return new SearchResultResponse(cityOnly, "📍 *Showing nearby properties in " + rl.city() + "*");
        }

        List<Property> relaxed = propertyService.findMatchesForSearchers(
            parsed.bhk(),
            rl.city(),
            null,
            null,
            parsed.maxBudget() != null ? parsed.maxBudget() + 20000 : null
        );

        if (!relaxed.isEmpty()) {
            return new SearchResultResponse(relaxed, "💡 *Showing slightly higher budget options*");
        }

        List<Property> broad = propertyService.findMatchesForSearchers(
            null,
            rl.city(),
            null,
            null,
            null
        );

        return new SearchResultResponse(broad, "🔎 *Showing best available properties in the city*");
    }

    private String formatAmount(Number amount) {
        if (amount == null) return "N/A";
        return String.format("%,d", amount.longValue());
    }
}