package com.risingbee.realestate.automation.service;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.SimpleParser;
import com.risingbee.realestate.automation.tenant.BrokerContext;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final LeadService leadService;
    private final PropertyService propertyService;
    private final WhatsAppSender whatsAppSender;

    public void handleIncoming(Map<String, Object> payload) {
        log.info("Handling incoming WhatsApp message...");

        if (BrokerContext.get() == null) {
            log.warn("No broker in context — cannot handle incoming message");
            return;
        }

        Optional<String> phoneOpt = extractPhone(payload);
        
        Optional<String> textOpt  = extractText(payload);
        
        


        if (phoneOpt.isEmpty() || textOpt.isEmpty()) {
            log.warn("Required fields missing from webhook payload");
            return;
        }

        String from = phoneOpt.get();
        String text = textOpt.get();

        ParsedRequest parsed = SimpleParser.parse(text);

        // 🚫 RENT-ONLY GUARD (THIS WAS MISSING)
        if (!parsed.valid()) {
            log.info("Invalid request detected: {}", parsed.invalidReason());

            if ("PURCHASE_BUDGET_NOT_SUPPORTED".equals(parsed.invalidReason())) {
                whatsAppSender.sendTextMessage(from,
                        """
                        I currently help with rental properties only.
                        Please share your monthly rent budget (e.g. 25k, 30k).
                        """
                );
            } else {
                whatsAppSender.sendTextMessage(from,
                        "Sorry, I couldn't understand your request. Please try again."
                );
            }
            return;
        }

        // ✅ Create lead only for valid rent intent
        leadService.createFromParsed(
                from,
                text,
                parsed.bhk(),
                parsed.minBudget(),
                parsed.maxBudget(),
                parsed.location()
        );

        Long brokerId = BrokerContext.id();

        List<Property> matches = propertyService.findMatches(
                parsed.bhk(),
                parsed.location(),
                parsed.minBudget(),
                parsed.maxBudget(),
                brokerId
        );

        if (matches.isEmpty()) {
            whatsAppSender.sendTextMessage(from,
                    """
                    I couldn't find matching properties yet.
                    You can try increasing your budget or changing location.
                    """
            );
            return;
        }

        // ✅ Return matching property list
        StringBuilder reply = new StringBuilder(
                "Here are some matching properties:\n\n"
        );

        matches.stream()
                .limit(5) // prevent spam
                .forEach(p -> {
                    reply.append("🏠 ").append(p.getTitle()).append('\n')
                         .append("📍 ").append(p.getArea()).append('\n')
                         .append("💰 ₹").append(p.getPrice()).append(" / month\n")
                         .append("---------------------\n");
                });

        reply.append("\nReply YES to connect with the broker.");

        whatsAppSender.sendTextMessage(from, reply.toString());
    }

    /* ---------------- Defensive JSON extractors ---------------- */

    private Optional<String> extractPhone(Map<String, Object> payload) {
        try {
            return firstMap(payload, "entry")
                    .flatMap(entry -> firstMap(entry, "changes"))
                    .flatMap(change -> map(change, "value"))
                    .flatMap(value -> firstMap(value, "messages"))
                    .flatMap(msg -> string(msg, "from"));
        } catch (Exception e) {
            log.debug("extractPhone failed", e);
            return Optional.empty();
        }
    }

    private Optional<String> extractText(Map<String, Object> payload) {
        try {
            return firstMap(payload, "entry")
                    .flatMap(entry -> firstMap(entry, "changes"))
                    .flatMap(change -> map(change, "value"))
                    .flatMap(value -> firstMap(value, "messages"))
                    .flatMap(msg -> map(msg, "text"))
                    .flatMap(text -> string(text, "body"));
        } catch (Exception e) {
            log.debug("extractText failed", e);
            return Optional.empty();
        }
    }

    /* ---------------- Small safe helpers ---------------- */

    private Optional<Map<String, Object>> map(Map<String, Object> src, String key) {
        Object val = src.get(key);
        if (val instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) m;
            return Optional.of(cast);
        }
        return Optional.empty();
    }

    private Optional<Map<String, Object>> firstMap(Map<String, Object> src, String key) {
        Object val = src.get(key);
        if (val instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) m;
            return Optional.of(cast);
        }
        return Optional.empty();
    }

    private Optional<String> string(Map<String, Object> src, String key) {
        Object val = src.get(key);
        return (val instanceof String s) ? Optional.of(s) : Optional.empty();
    }
}
