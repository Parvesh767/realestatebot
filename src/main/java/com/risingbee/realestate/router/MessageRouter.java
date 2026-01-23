package com.risingbee.realestate.router;

import org.springframework.stereotype.Component;

@Component
public class MessageRouter {

    public Route resolve(String message, boolean isBroker) {

        if (!isBroker || message == null) {
            return Route.UNKNOWN;
        }

        String text = message.trim().toLowerCase();

        if (text.startsWith("add")) {
            return Route.ADD_PROPERTY;
        }

        return Route.UNKNOWN;
    }

    public enum Route {
        ADD_PROPERTY,
        UNKNOWN
    }
}
