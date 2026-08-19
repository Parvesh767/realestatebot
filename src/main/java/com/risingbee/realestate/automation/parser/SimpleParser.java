package com.risingbee.realestate.automation.parser;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleParser {

    private static final Pattern BHK_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.5)?)\\s*(bhk|bedroom|bed)");
    private static final Pattern CRORE_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(cr|crore|crores)");
    private static final Pattern LAKH_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(l|lac|lakh|lakhs)");
    private static final Pattern THOUSAND_PATTERN = Pattern.compile("(?i)(\\d+)\\s*(k|thousand)");

    public static ParsedRequest parse(String message) {
        if (message == null || message.isBlank()) {
            return new ParsedRequest(null, null, null, List.of(), "", List.of(), message);
        }

        String normalized = message.toLowerCase().replaceAll("[^a-z0-9.]", " ").replaceAll("\\s+", " ").trim();
        List<String> tokens = List.of(normalized.split(" "));

        String bhk = extractBhk(normalized);
        Double[] budget = extractBudget(normalized); // [min, max]

        List<String> locationCandidates = extractLocationTokens(tokens);

        return new ParsedRequest(
            bhk,
            budget[0] != null ? budget[0].longValue() : null,
            budget[1] != null ? budget[1].longValue() : null,
            locationCandidates,
            normalized,
            tokens,
            message
        );
    }

    private static String extractBhk(String text) {
        Matcher m = BHK_PATTERN.matcher(text);
        return m.find() ? m.group(1) + "BHK" : null;
    }

    private static Double[] extractBudget(String text) {
        Double min = null;
        Double max = null;

        Matcher crMatcher = CRORE_PATTERN.matcher(text);
        if (crMatcher.find()) {
            max = Double.parseDouble(crMatcher.group(1)) * 10_000_000;
        }

        Matcher lakhMatcher = LAKH_PATTERN.matcher(text);
        if (max == null && lakhMatcher.find()) {
            max = Double.parseDouble(lakhMatcher.group(1)) * 100_000;
        }

        Matcher kMatcher = THOUSAND_PATTERN.matcher(text);
        if (max == null && kMatcher.find()) {
            max = Double.parseDouble(kMatcher.group(1)) * 1_000;
        }

        return new Double[]{min, max};
    }

    private static List<String> extractLocationTokens(List<String> tokens) {
        Set<String> ignore = Set.of("bhk", "under", "below", "near", "flat", "house", "budget", "in", "for", "cr", "lakh");
        return tokens.stream()
                .filter(t -> t.length() >= 3)
                .filter(t -> !t.matches("\\d+.*"))
                .filter(t -> !ignore.contains(t))
                .toList();
    }
}