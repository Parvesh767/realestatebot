package com.risingbee.realestate.automation.parser;


import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SimpleParser {

    // Budget like: 25k, 30k, 45,000, 50000
    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("\\b(\\d{2,5})(k|K)?\\b");

    private static final Pattern BHK_PATTERN =
            Pattern.compile("(\\d)\\s*(bhk|BHK)");

    private static final Map<String, String> LOCATION_MAP = Map.of(
    	    "golf course road", "Golf Course Road",
    	    "golf", "Golf Course Road",
    	    "dlf", "DLF",
    	    "sohna", "Sohna",
    	    "gurgaon", "Gurgaon",
    	    "gurugram", "Gurugram"
    	);
    public static ParsedRequest parse(String message) {

        if (message == null || message.isBlank()) {
            return ParsedRequest.empty();
        }

        String normalized = message.toLowerCase().trim();
        log.info("Parsing message: {}", normalized);

        // 🚫 Rent-only guard
        if (normalized.contains("lakh") || normalized.contains("crore")) {
            return ParsedRequest.invalid(
                    "PURCHASE_BUDGET_NOT_SUPPORTED"
            );
        }

        String bhk = extractBhk(normalized);
        Integer[] budget = extractBudget(normalized);
        String location = extractLocation(normalized);

        return new ParsedRequest(
                bhk,
                budget[0],
                budget[1],
                location,
                true,
                null
        );
    }

    private static String extractBhk(String text) {
        Matcher m = BHK_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1) + "BHK";
        }
        return null;
    }

    private static Integer[] extractBudget(String text) {

        Matcher m = BUDGET_PATTERN.matcher(text);
        Integer min = null;
        Integer max = null;

        if (!m.find()) {
            return new Integer[]{null, null};
        }

        int value = Integer.parseInt(m.group(1));
        if (m.group(2) != null) {
            value = value * 1000;
        }

        // Intent-aware logic
        if (isUnderIntent(text)) {
            min = null;
            max = value;
        }
        else if (isAboveIntent(text)) {
            min = value;
            max = null;
        }
        else {
            // No intent word → treat as exact or upper bound
            min = null;
            max = value;
        }

        return new Integer[]{min, max};
    }


    private static String extractLocation(String text) {
        for (var entry : LOCATION_MAP.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    private static boolean isUnderIntent(String text) {
        return text.contains("under")
            || text.contains("below")
            || text.contains("less than");
    }

    private static boolean isAboveIntent(String text) {
        return text.contains("above")
            || text.contains("over")
            || text.contains("more than");
    }
    
    
}


