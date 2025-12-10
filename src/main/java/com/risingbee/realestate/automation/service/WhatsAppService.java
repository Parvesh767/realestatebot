package com.risingbee.realestate.automation.service;

import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.SimpleParser;
import com.risingbee.realestate.automation.tenant.BrokerContext;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.domain.Lead;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final LeadService leadService;
    private final PropertyService propertyService;
    private final WhatsAppSender whatsAppSender;

    /**
     * payload = raw webhook JSON map
     */
    public void handleIncoming(Map<String, Object> payload) {
        log.info("Handling incoming WhatsApp message...");

        // 1. Ensure a broker is present in the context
        if (BrokerContext.get() == null) {
            log.warn("No broker in context — cannot handle incoming message");
            return;
        }

        // 2. Safe extraction of phone/text
        Optional<String> phoneOpt = safeExtractPhone(payload);
        Optional<String> textOpt = safeExtractText(payload);

        if (phoneOpt.isEmpty() || textOpt.isEmpty()) {
            log.warn("Required fields missing from webhook payload");
            return;
        }

        String from = phoneOpt.get();
        String text = textOpt.get();

        log.info("Incoming message → from: {}, text: {}", from, text);

        // 3. Parse
        ParsedRequest parsed = SimpleParser.parse(text);
        log.info("Parsed → bhk={}, min={}, max={}, location={}",
                parsed.bhk(), parsed.minBudget(), parsed.maxBudget(), parsed.location());

        // 4. Save lead
        Lead lead = leadService.createFromParsed(from, text, parsed.bhk(), parsed.minBudget(), parsed.maxBudget(), parsed.location());
        log.info("Lead saved successfully with ID {}", lead.getId());

        // 5. Find matches limited to current broker
        Long brokerId = BrokerContext.id();
        List<Property> matches = propertyService.findMatches(parsed.bhk(), parsed.location(), parsed.minBudget(), parsed.maxBudget(), brokerId);

        // 6. Send response
        if (matches.isEmpty()) {
            String msg = """
                    I couldn't find matching properties for your requirement yet.
                    Could you refine the budget, BHK, or preferred location?
                    """;
            whatsAppSender.sendTextMessage(from, msg);
            log.info("Sent fallback reply — no matches found");
            return;
        }

        StringBuilder reply = new StringBuilder("Here are the best matching properties for you:\n\n");
        for (Property p : matches) {
            reply.append(p.getTitle()).append("\n")
                 .append("Location: ").append(p.getArea()).append("\n")
                 .append("Price: ₹").append(p.getPrice()).append("\n")
                 .append("-----------------------\n");
        }
        whatsAppSender.sendTextMessage(from, reply.toString());
        log.info("Sent property recommendations to {}", from);
    }

    // Defensive helpers
    private Optional<String> safeExtractPhone(Map<String, Object> payload) {
        try {
            var entryList = (List<?>) payload.getOrDefault("entry", List.of());
            var entry = entryList.stream().findFirst().orElse(null);
            if (!(entry instanceof Map<?,?> entryMap)) return Optional.empty();

            var changeList = (List<?>) entryMap.getOrDefault("changes", List.of());
            var change = changeList.stream().findFirst().orElse(null);
            if (!(change instanceof Map<?,?> changeMap)) return Optional.empty();

            var value = changeMap.get("value");
            if (!(value instanceof Map<?,?> valueMap)) return Optional.empty();

            var messagesList = (List<?>) valueMap.getOrDefault("messages", List.of());
            var msg = messagesList.stream().findFirst().orElse(null);
            if (!(msg instanceof Map<?,?> message)) return Optional.empty();

            return Optional.ofNullable((String) message.get("from"));

        } catch (Exception e) {
            log.debug("safeExtractPhone failed", e);
            return Optional.empty();
        }
    }
    private Optional<String> safeExtractText(Map<String, Object> payload) {
        try {
            Object entry = ((java.util.List<?>) payload.getOrDefault("entry", java.util.List.of())).stream().findFirst().orElse(null);
            if (entry == null) return Optional.empty();
            Map<?,?> entryMap = (Map<?,?>) entry;
            Object change = ((java.util.List<?>) entryMap.getOrDefault("changes", java.util.List.of())).stream().findFirst().orElse(null);
            if (change == null) return Optional.empty();
            Map<?,?> changeMap = (Map<?,?>) change;
            Map<?,?> value = (Map<?,?>) changeMap.get("value");
            if (value == null) return Optional.empty();
            Object messages = ((java.util.List<?>) value.getOrDefault("messages", java.util.List.of())).stream().findFirst().orElse(null);
            if (messages == null) return Optional.empty();
            Map<?,?> message = (Map<?,?>) messages;
            Map<?,?> text = (Map<?,?>) message.get("text");
            if (text == null) return Optional.empty();
            return Optional.ofNullable((String) text.get("body"));
        } catch (Exception e) {
            log.debug("safeExtractText failed", e);
            return Optional.empty();
        }
    }
}
