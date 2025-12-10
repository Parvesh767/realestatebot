package com.risingbee.realestate.automation.match;


import java.util.List;

import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.parser.ParsedRequest;


public class MatchingEngine {

    public static int score(Property p, ParsedRequest req) {
        int score = 0;

        // BHK match
        if (req.bhk() != null && p.getBhk() != null) {
            if (p.getBhk().equalsIgnoreCase(req.bhk())) {
                score += 40;
            } else if (p.getBhk().startsWith(req.bhk().substring(0, 1))) {
                score += 20;
            }
        }

        // Budget match
        if (req.minBudget() != null && req.maxBudget() != null) {
            if (p.getPrice() >= req.minBudget() && p.getPrice() <= req.maxBudget()) {
                score += 30;
            } else if (Math.abs(p.getPrice() - req.maxBudget()) <= 5000) {
                score += 10;
            }
        }

        // Location match
        if (req.location() != null && p.getArea() != null) {
            if (p.getArea().toLowerCase().contains(req.location().toLowerCase())) {
                score += 30;
            }
        }

        return score;
    }

    public static List<Property> topMatches(List<Property> props, ParsedRequest req, int limit) {
        return props.stream()
                .sorted((a, b) -> score(b, req) - score(a, req))
                .limit(limit)
                .toList();
    }
}
