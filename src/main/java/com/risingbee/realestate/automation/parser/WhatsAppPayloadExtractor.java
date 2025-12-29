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

	
//	private final WhatsAppService whatsAppService;
//    @SuppressWarnings("unchecked")
//    public Optional<String> extractPhone(Map<String, Object> payload) {
//        try {
//            List<Map<String, Object>> entry =
//                    (List<Map<String, Object>>) payload.get("entry");
//            if (entry == null || entry.isEmpty()) return Optional.empty();
//
//            Map<String, Object> changes =
//                    (Map<String, Object>) ((List<?>) entry.get(0).get("changes")).get(0);
//            Map<String, Object> value =
//                    (Map<String, Object>) changes.get("value");
//
//            List<Map<String, Object>> messages =
//                    (List<Map<String, Object>>) value.get("messages");
//            if (messages == null || messages.isEmpty()) return Optional.empty();
//
//            return Optional.ofNullable((String) messages.get(0).get("from"));
//        } catch (Exception e) {
//            log.warn("Failed to extract phone from WhatsApp payload", e);
//            return Optional.empty();
//        }
//    }
//  
    
    
    
	public Optional<String> extractPhone(Map<String, Object> payload) {
		try {
			return firstMap(payload, "entry").flatMap(entry -> firstMap(entry, "changes"))
					.flatMap(change -> map(change, "value")).flatMap(value -> firstMap(value, "messages"))
					.flatMap(msg -> string(msg, "from"));
		} catch (Exception e) {
			log.debug("extractPhone failed", e);
			return Optional.empty();
		}
	}

	public Optional<String> extractText(Map<String, Object> payload) {
		try {
			return firstMap(payload, "entry").flatMap(entry -> firstMap(entry, "changes"))
					.flatMap(change -> map(change, "value")).flatMap(value -> firstMap(value, "messages"))
					.flatMap(msg -> map(msg, "text")).flatMap(text -> string(text, "body"));
		} catch (Exception e) {
			log.debug("extractText failed", e);
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

	
	@SuppressWarnings("unchecked")
	public Optional<MediaInput> extractImageMedia(Map<String, Object> payload) {

	    try {
	        List<Map<String, Object>> entry =
	            (List<Map<String, Object>>) payload.get("entry");
	        if (entry == null || entry.isEmpty()) return Optional.empty();

	        List<Map<String, Object>> changes =
	            (List<Map<String, Object>>) entry.get(0).get("changes");
	        if (changes == null || changes.isEmpty()) return Optional.empty();

	        Map<String, Object> value =
	            (Map<String, Object>) changes.get(0).get("value");

	        List<Map<String, Object>> messages =
	            (List<Map<String, Object>>) value.get("messages");
	        if (messages == null || messages.isEmpty()) return Optional.empty();

	        Map<String, Object> msg = messages.get(0);

	        if (!"image".equals(msg.get("type"))) {
	            return Optional.empty();
	        }

	        Map<String, Object> image =
	            (Map<String, Object>) msg.get("image");

	        String mediaId = (String) image.get("id");
	        String mimeType = (String) image.get("mime_type");

	        if (mediaId == null) return Optional.empty();

	        return Optional.of(new MediaInput(mediaId, mimeType));

	    } catch (Exception e) {
	        log.warn("Failed to extract image media", e);
	        return Optional.empty();
	    }
	}
	
    @SuppressWarnings("unchecked")
    public boolean isUserMessageEvent(Map<String, Object> payload) {

        try {
            List<Map<String, Object>> entry =
                (List<Map<String, Object>>) payload.get("entry");
            if (entry == null || entry.isEmpty()) return false;

            List<Map<String, Object>> changes =
                (List<Map<String, Object>>) entry.get(0).get("changes");
            if (changes == null || changes.isEmpty()) return false;

            Map<String, Object> value =
                (Map<String, Object>) changes.get(0).get("value");

            return value.containsKey("messages");

        } catch (Exception e) {
            return false;
        }
    }



	
}
