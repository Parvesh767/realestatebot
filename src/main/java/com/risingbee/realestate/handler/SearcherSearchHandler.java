package com.risingbee.realestate.handler;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.parser.LocationResolver;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.ResolvedLocation;
import com.risingbee.realestate.automation.service.LeadService;
import com.risingbee.realestate.automation.service.PropertyService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
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
    private final LocationResolver locationResolver;
    private final LocationDisplayService lcoalDisplayService;

    public void handle(Actor actor, ParsedRequest parsed) {

        String from = actor.externalId();

        if (!parsed.hasSearchIntent()) {
            whatsAppSender.sendTextMessage(
                from,
                "Please send a valid requirement, e.g.\n2 BHK rent in Gurgaon under 30k"
            );
            return;
        }

        ResolvedLocation rl = locationResolver.resolve(parsed).orElse(null);

        if (rl == null || rl.city() == null) {
            whatsAppSender.sendTextMessage(from, "❌ Please mention a valid location.");
            return;
        }

        List<Property> matches =
            propertyService.findMatchesForSearchers(
                parsed.bhk(),
                rl.city(),
                rl.locality(),
                parsed.minBudget(),
                parsed.maxBudget()
            );

        if (matches.isEmpty()) {
            whatsAppSender.sendTextMessage(
                from,
                "❌ No matching properties found.\nTry changing location or budget."
            );
            return;
        }

        Map<Long, List<Property>> byAccount =
            matches.stream()
                   .collect(Collectors.groupingBy(Property::getOwnerAccountId));

        for (Map.Entry<Long, List<Property>> entry : byAccount.entrySet()) {
            Property ref = entry.getValue().get(0);

            Lead lead = leadService.createFromMatchedProperty(
                from,
                ref,
                parsed.normalizedText(),
                rl.city(),
                rl.locality()
            );

            log.info(
                "✅ Lead created. leadId={}, ownerAccountId={}",
                lead.getId(),
                ref.getOwnerAccountId()
            );
        }

        sendResults(from, matches);
    }

    private void sendResults(String phone, List<Property> properties) {
        StringBuilder msg = new StringBuilder("🏠 *Available Properties*\n\n");

        int count = 0;
        for (Property p : properties) {
            String locality =
                lcoalDisplayService.localityName(p.getLocalityCode());

            msg.append("• ")
               .append(p.getBhk())
               .append(" in ")
               .append(locality)
               .append("\n💰 ₹")
               .append(formatAmount(p.getPrice()))
               .append("\n\n");

            if (++count == 5) break;
        }

        msg.append("Reply *YES* to connect.");

        whatsAppSender.sendTextMessage(phone, msg.toString());
    }

    private String formatAmount(Integer amount) {
        return String.format("%,d", amount);
    }
}
