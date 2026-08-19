package com.risingbee.realestate.automation.match;

import java.util.List;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.ResolvedLocation;

public class MatchingEngine {

    public static int score(Property p, ParsedRequest req, ResolvedLocation location) {
        int score = 0;

        // 1. Location Match (Highest Priority: 50 points)
        if (location != null && p.getLocalityCode() != null) {
            if (p.getLocalityCode().equalsIgnoreCase(location.locality())) {
                score += 50;
            } else if (p.getCityCode() != null && p.getCityCode().equalsIgnoreCase(location.city())) {
                score += 25; // Fallback city match
            }
        }

        // 2. BHK Match (30 points)
        if (req.bhk() != null && p.getBhk() != null) {
            if (p.getBhk().equalsIgnoreCase(req.bhk())) {
                score += 30;
            }
        }

        // 3. Price/Budget Match (20 points)
        if (req.maxBudget() != null && p.getPrice() != null) {
            if (p.getPrice() <= req.maxBudget()) {
                score += 20;
            } else if (p.getPrice() <= req.maxBudget() * 1.1) { 
                // Allow a 10% margin above budget
                score += 10;
            }
        }

        return score;
    }

    public static List<Property> topMatches(List<Property> props, ParsedRequest req, ResolvedLocation location, int limit) {
        return props.stream()
                .filter(p -> score(p, req, location) > 30) // Filter out weak matches
                .sorted((a, b) -> Integer.compare(score(b, req, location), score(a, req, location)))
                .limit(limit)
                .toList();
    }
}