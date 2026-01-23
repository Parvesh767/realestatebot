package com.risingbee.realestate.flow;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class FlowResolver {

    public Optional<ConversationFlow> resolve(String text) {

        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String t = text.trim().toLowerCase();

        if (isGreeting(t)) {
            return Optional.empty();
        }

        if (isBrokerIntent(t)) {
            return Optional.of(ConversationFlow.BROKER_ONBOARDING);
        }

        if (isSearchIntent(t)) {
            return Optional.of(ConversationFlow.SEARCH);
        }

        return Optional.empty();
    }

    private boolean isGreeting(String t) {
    		t  = t.toLowerCase();
        return Set.of("hi","hii","hello","hey","ok").contains(t);
    }

    private boolean isBrokerIntent(String m) {
        return m.toLowerCase().contains("broker") ||
                m.toLowerCase().contains("agent") ||
                m.toLowerCase().contains("owner") ||
                m.toLowerCase().contains("list my property") ||
                m.toLowerCase().contains("add property") ||
                m.toLowerCase().contains("post property") ||
                m.toLowerCase().contains("rent my flat") ||
                m.toLowerCase().contains("sell my flat");
    }

    private boolean isSearchIntent(String t) {
        return t.toLowerCase().contains("looking")
            || t.toLowerCase().contains("need")
            || t.toLowerCase().contains("bhk")
            || t.toLowerCase().contains("rent")
            || t.toLowerCase().contains("under") 
            || t.toLowerCase().contains("in ");
    }
    
    

    public Optional<ConversationFlow> resolveExplicitChoice(String message) {

        String m = message.toLowerCase().trim();

        // Numeric shortcuts (still supported)
        if (m.equals("1")) return Optional.of(ConversationFlow.SEARCH);
        if (m.equals("2")) return Optional.of(ConversationFlow.BROKER_ONBOARDING);

        // Natural language triggers for onboarding
        if (isBrokerIntent(message)) {
            return Optional.of(ConversationFlow.BROKER_ONBOARDING);
        }

        // Natural language search triggers (optional)
        if (isSearchIntent(message)) {
            return Optional.of(ConversationFlow.SEARCH);
        }

        return Optional.empty();
    }

}

