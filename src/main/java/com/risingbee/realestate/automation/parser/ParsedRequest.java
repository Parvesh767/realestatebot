package com.risingbee.realestate.automation.parser;


public record ParsedRequest(
        String bhk,
        Integer minBudget,
        Integer maxBudget,
        String location
) { }

