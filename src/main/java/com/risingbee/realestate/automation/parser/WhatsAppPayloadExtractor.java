package com.risingbee.realestate.automation.parser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.risingbee.realestate.automation.dto.MediaInput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WhatsAppPayloadExtractor {

    public Optional<String> extractPhone(Map<String, Object> payload) {
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

    public Optional<String> extractText(Map<String, Object> payload) {
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

    public Optional<String> extractMessageId(Map<String, Object> payload) {
        try {
            return firstMap(payload, "entry")
                    .flatMap(entry -> firstMap(entry, "changes"))
                    .flatMap(change -> map(change, "value"))
                    .flatMap(value -> firstMap(value, "messages"))
                    .flatMap(msg -> string(msg, "id"));
        } catch (Exception e) {
            log.debug("extractMessageId failed", e);
            return Optional.empty();
        }
    }

    public boolean isUserMessageEvent(Map<String, Object> payload) {
        try {
            return firstMap(payload, "entry")
                    .flatMap(entry -> firstMap(entry, "changes"))
                    .flatMap(change -> map(change, "value"))
                    .map(value -> value.containsKey("messages"))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<MediaInput> extractImageMedia(Map<String, Object> payload) {
        try {
            Optional<Map<String, Object>> msgOpt = firstMap(payload, "entry")
                    .flatMap(entry -> firstMap(entry, "changes"))
                    .flatMap(change -> map(change, "value"))
                    .flatMap(value -> firstMap(value, "messages"));

            if (msgOpt.isEmpty()) return Optional.empty();

            Map<String, Object> msg = msgOpt.get();
            String type = (String) msg.get("type");
            if (!"image".equals(type)) {
                return Optional.empty();
            }

            Optional<Map<String, Object>> imageOpt = map(msg, "image");
            if (imageOpt.isEmpty()) return Optional.empty();

            Map<String, Object> image = imageOpt.get();
            String mediaId = (String) image.get("id");
            String mimeType = (String) image.get("mime_type");

            if (mediaId == null) return Optional.empty();

            return Optional.of(new MediaInput(mediaId, mimeType));

        } catch (Exception e) {
            log.warn("Failed to extract image media", e);
            return Optional.empty();
        }
    }

    /* ---------------- Small safe helpers ---------------- */

    public Optional<Map<String, Object>> map(Map<String, Object> src, String key) {
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