package com.risingbee.realestate.automation.parser;


import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SimpleParser {

    // Budget like: 25k, 30k, 45,000, 50000
    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("\\b(\\d{2,5})(k|K)?\\b");

    private static final Pattern BHK_PATTERN =
            Pattern.compile("(\\d)\\s*(bhk|BHK)");

    private static final String[] LOCATIONS = {
            "gurgaon", "gurugram", "sector", "sohna", "dlf"
    };

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

        while (m.find()) {
            int value = Integer.parseInt(m.group(1));

            if (m.group(2) != null) {
                value = value * 1000; // k → thousands
            }

            if (min == null) min = value;
            else max = value;
        }

        if (min != null && max == null) {
            max = min;
        }

        return new Integer[]{min, max};
    }

    private static String extractLocation(String text) {
        for (String loc : LOCATIONS) {
            if (text.contains(loc)) {
                return loc;
            }
        }
        return null;
    }
}


