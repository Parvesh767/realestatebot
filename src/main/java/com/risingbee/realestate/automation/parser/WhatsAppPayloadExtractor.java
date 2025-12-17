package com.risingbee.realestate.automation.parser;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WhatsAppPayloadExtractor {

    @SuppressWarnings("unchecked")
    public Optional<String> extractPhone(Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entry =
                    (List<Map<String, Object>>) payload.get("entry");
            if (entry == null || entry.isEmpty()) return Optional.empty();

            Map<String, Object> changes =
                    (Map<String, Object>) ((List<?>) entry.get(0).get("changes")).get(0);
            Map<String, Object> value =
                    (Map<String, Object>) changes.get("value");

            List<Map<String, Object>> messages =
                    (List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty()) return Optional.empty();

            return Optional.ofNullable((String) messages.get(0).get("from"));
        } catch (Exception e) {
            log.warn("Failed to extract phone from WhatsApp payload", e);
            return Optional.empty();
        }
    }
}
