package com.risingbee.realestate.automation.parser;


import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleParser {

    // Budget like: 25k, 30k, 45,000, 50000
    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("\\b(\\d{2,7})(k|K)?\\b");

    private static final Pattern BHK_PATTERN =
    		Pattern.compile("(\\d+(?:\\.5)?)\\s*(bhk|bedroom|bed)");


     private static final Map<String, String> LOCALITY_ALIASES = Map.of(
    	    "golf course road", "Golf Course Road",
    	    "golf", "Golf Course Road",
    	    "dlf", "DLF",
    	    "sohna", "Sohna"
    	);

    
    private static final Map<String, String> CITY_ALIASES = Map.ofEntries(
    	    Map.entry("gurgaon", "GURGAON"),
    	    Map.entry("gurugram", "GURGAON"),
    	    Map.entry("ggn", "GURGAON"),

    	    Map.entry("noida", "NOIDA"),
    	    Map.entry("greater noida", "NOIDA"),

    	    Map.entry("delhi", "DELHI")
    	);

    
    private static final List<String> fillers = List.of(
    	    "need", "want", "looking", "for", "in", "near", "please", "flat", "house"
    	);

    
    public static ParsedRequest parse(String message) {
    	  	
    	
    	  if (message == null || message.isBlank()) {
              return ParsedRequest.empty();
          }
    	
    	String msg = message.toLowerCase();
    	msg = msg.replaceAll("[^a-z0-9 ]", " ");
    	msg = msg.replaceAll("\\s+", " ").trim();
    	
    	
    	log.info("Parsing normalized message: {}", msg);
    	

    	
    	String title = extractTitle(msg);
        String bhk = extractBhk(msg);
        Integer[] budget = extractBudget(msg);
        String location = extractLocation(msg);
    	
    
        
	String city = null;
    	
    	for (Map.Entry<String, String> entry : CITY_ALIASES.entrySet()) {
    	    if (msg.contains(entry.getKey())) {
    	        city = entry.getValue();   // canonical
    	        msg = msg.replace(entry.getKey(), "").trim();
    	        break;
    	    }
    	}

    	
    	for (String f : fillers) {
    	    msg = msg.replace(" " + f + " ", " ");
    	}
    	
    	
    

      

//        String normalized = message.toLowerCase().trim();
//        log.info("Parsing message: {}", normalized);
//
//        // 🚫 Rent-only guard
//        if (normalized.contains("lakh") || normalized.contains("crore")) {
//            return ParsedRequest.invalid(
//                    "PURCHASE_BUDGET_NOT_SUPPORTED"
//            );
//        }
//	
        
        
        
        log.info(
        	    "Parsed intent → city={}, bhk={}, minBudget={}, maxBudget={}, location={}",
        	    city, bhk, budget[0], budget[1], location
        	);

        return new ParsedRequest(
        		title,
                bhk,
                budget[0],
                budget[1],
                location,
                city,
                true,
                null
        );
    }

    
    private static String extractTitle(String text) {
    	
    	return extractBhk(text) + extractLocation(text);
    	
    }
    private static String extractBhk(String text) {
        Matcher m = BHK_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1) + "BHK";
        }
        return null;
    }

    private static Integer[] extractBudget(String text) {

        Matcher m = BUDGET_PATTERN.matcher(text);
        Integer min = null;
        Integer max = null;

        if (!m.find()) {
            return new Integer[]{null, null};
        }

        int value = Integer.parseInt(m.group(1));
        if (m.group(2) != null) {
            value = value * 1000;
        }

        // Intent-aware logic
        if (isUnderIntent(text)) {
            min = null;
            max = value;
        }
        else if (isAboveIntent(text)) {
            min = value;
            max = null;
        }
        else {
            // No intent word → treat as exact or upper bound
            min = null;
            max = value;
        }

        return new Integer[]{min, max};
    }


    private static String extractLocation(String text) {
        for (var entry : LOCALITY_ALIASES.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    private static boolean isUnderIntent(String text) {
        return text.contains("under")
            || text.contains("below")
            || text.contains("less than");
    }

    private static boolean isAboveIntent(String text) {
        return text.contains("above")
            || text.contains("over")
            || text.contains("more than");
    }
    
    
}


