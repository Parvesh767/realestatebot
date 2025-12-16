package com.risingbee.realestate.automation.parser;


public record ParsedRequest(
        String bhk,
        Integer minBudget,
        Integer maxBudget,
        String location,
        boolean valid,
        String invalidReason
) {
    public static ParsedRequest invalid(String reason) {
        return new ParsedRequest(null, null, null, null, false, reason);
    }

    public static ParsedRequest empty() {
        return new ParsedRequest(null, null, null, null, true, null);
    }
}


