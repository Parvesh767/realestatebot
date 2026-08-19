package com.risingbee.realestate.automation.parser;

import java.util.List;

public record ParsedRequest(

    String bhk,

    Long minBudget,  // Updated from Integer to Long to handle Cr/Lakh amounts

    Long maxBudget,  // Updated from Integer to Long to handle Cr/Lakh amounts

    List<String> location,

    String normalizedText,

    List<String> tokens,

    String msg

) {

    public boolean hasBudget() {
        return minBudget != null || maxBudget != null;
    }

    public boolean hasBhk() {
        return bhk != null;
    }

    public boolean hasLocation() {
        return location != null && !location.isEmpty();
    }

    public boolean hasListingSignals() {
        return hasBhk()
            || hasBudget()
            || hasLocation();
    }

    public boolean hasSearchIntent() {
        int signals = 0;
        if (hasBhk()) signals++;
        if (hasBudget()) signals++;
        if (hasLocation()) signals++;

        return signals >= 2;
    }
}