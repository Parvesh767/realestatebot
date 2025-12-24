package com.risingbee.realestate.automation.parser;


public record ParsedRequest(
		String title,
        String bhk,
        Integer minBudget,
        Integer maxBudget,
        String location,
        String city,
        boolean valid,
        String invalidReason
) {
    public static ParsedRequest invalid(String reason) {
        return new ParsedRequest(null,null, null, null, null,null, false, reason);
    }
    

    public static ParsedRequest empty() {
        return new ParsedRequest(null,null, null, null, null,null, true, null);
    }
    
    public boolean hasSearchIntent() {
        return bhk != null
            && (city != null || location != null)
            && (minBudget != null || maxBudget != null);
    }
}


