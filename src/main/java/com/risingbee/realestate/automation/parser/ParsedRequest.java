package com.risingbee.realestate.automation.parser;

import java.util.List;
public record ParsedRequest(
	    String bhk,
	    Integer minBudget,
	    Integer maxBudget,

	    /** Raw, fuzzy location-like text (e.g. "sector 56", "golf") */
	    List<String> location,

	    String normalizedText,
	    List<String> tokens,

	    boolean hasSearchIntent
	) {

	    public boolean hasBudget() {
	        return minBudget != null || maxBudget != null;
	    }

	    public boolean hasBhk() {
	        return bhk != null;
	    }

	    public boolean hasLocation() {
	        return !location.isEmpty();
	    }

	    public boolean hasListingSignals() {
	        return hasBhk() || hasBudget() || hasLocation();
	    }
	    
	    public  boolean hasSearchIntent() {
	        return  bhk != null &&
	                (minBudget != null || maxBudget != null) &&
	                !location.isEmpty() ;
	    }
	}

