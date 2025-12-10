package com.risingbee.realestate.automation.parser;


import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SimpleParser {

    // Regex to detect budget like: 25k, 30k, 45,000, 50000
    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("(\\d{2,3})(k|K|000)?");

    // Regex to detect BHK: 1bhk, 2bhk, 3bhk
    private static final Pattern BHK_PATTERN =
            Pattern.compile("(\\d)\\s*(bhk|BHK)");

    // Simple location hints (we’ll improve later)
    private static final String[] LOCATIONS = {
            "gurgaon", "gurugram", "sector", "sohna", "dlf"
    };

    public static ParsedRequest parse(String message) {
        if (message == null) {
            return new ParsedRequest(null, null, null, null);
        }

        message = message.toLowerCase().trim();
        log.info("Parsing message: {}", message);

        // Extract values
        String bhk = extractBhk(message);
        Integer[] budget = extractBudget(message);
        String location = extractLocation(message);

        return new ParsedRequest(
                bhk,
                budget[0],
                budget[1],
                location
        );
    }

    // ---------------------------------------------
    // BHK Extractor
    // ---------------------------------------------
    private static String extractBhk(String text) {
        Matcher m = BHK_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1) + "BHK";
        }
        return null;
    }

    // ---------------------------------------------
    // Budget Extractor
    // ---------------------------------------------
    private static Integer[] extractBudget(String text) {

        Matcher m = BUDGET_PATTERN.matcher(text);
        Integer min = null;
        Integer max = null;

        while (m.find()) {
            String number = m.group(1);
            String suffix = m.group(2);

            int value = Integer.parseInt(number);

            if (suffix != null && suffix.equalsIgnoreCase("k")) {
                value = value * 1000;
            }

            if (suffix != null && suffix.equals("000")) {
                value = value;
            }

            if (min == null) min = value;
            else max = value;
        }

        // If only one budget is found, min = max
        if (min != null && max == null) {
            max = min;
        }

        return new Integer[]{min, max};
    }

    // ---------------------------------------------
    // Location Extractor
    // ---------------------------------------------
    private static String extractLocation(String text) {
        for (String loc : LOCATIONS) {
            if (text.contains(loc)) {
                return loc;
            }
        }
        return null;
    }
}
