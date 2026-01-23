package com.risingbee.realestate.automation.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleParser {

    private static final Pattern BHK_PATTERN =
            Pattern.compile("(\\d+(?:\\.5)?)\\s*(bhk|bedroom|bed)");

    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("(under|below|less than|over|above|more than)?\\s*(\\d{2,7})(k|K)?");

    public static ParsedRequest parse(String message) {

        if (message == null || message.isBlank()) {
            return new ParsedRequest(
                null,
                null,
                null,
                null,
                "",
                List.of(),
                false
            );
        }

        String normalized = normalize(message);
        List<String> tokens = List.of(normalized.split(" "));

        String bhk = extractBhk(normalized);
        Integer[] budget = extractBudget(normalized);

        // 🔑 weak, fuzzy location signal
        List<String> location = extractLocationCandidates(tokens);

        // Search intent = structured requirement
        boolean hasSearchIntent =
            bhk != null &&
            (budget[0] != null || budget[1] != null) &&
            location.isEmpty() ;

        log.info(
            "Parsed → bhk={}, min={}, max={}, location={}, tokens={}",
            bhk, budget[0], budget[1], location, tokens
        );

        return new ParsedRequest(
            bhk,
            budget[0],
            budget[1],
            location,
            normalized,
            tokens,
            hasSearchIntent
        );
    }

    
    private static List<String> extractLocationCandidates(List<String> tokens) {

        return tokens.stream()
            .map(String::toLowerCase)
            .filter(t -> t.length() >= 2)
            .filter(t -> extractBhk(t) == null)
            .filter(t -> !t.matches("\\d+k|\\d{4,6}"))   // ✅ FIX
            .filter(t -> !Set.of(
                "rent", "under", "below", "near",
                "flat", "house", "budget",
                "price", "looking"
            ).contains(t))
            .toList();
    }



    private static String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String extractBhk(String text) {
        Matcher m = BHK_PATTERN.matcher(text);
        return m.find() ? m.group(1) + "BHK" : null;
    }

    private static Integer[] extractBudget(String text) {
        Matcher m = BUDGET_PATTERN.matcher(text);

        Integer min = null;
        Integer max = null;

        if (!m.find()) return new Integer[]{null, null};

        String intent = m.group(1);
        int value = Integer.parseInt(m.group(2));
        if (m.group(3) != null) value *= 1000;

        if (intent == null || intent.contains("under") || intent.contains("below")) {
            max = value;
        } else {
            min = value;
        }

        return new Integer[]{min, max};
    }
}
