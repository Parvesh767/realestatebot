package com.risingbee.realestate.router;

import org.springframework.stereotype.Component;

@Component
public class MessageRouter {

    public Route resolve(String message, boolean isBroker) {

        if (isBroker && message != null && message.toLowerCase().startsWith("add")) {
            return Route.ADD_PROPERTY;
        }

        // broker-only commands
        if (message.startsWith("add property")) {
            return Route.ADD_PROPERTY;
        }
        return Route.UNKNOWN;
    }

    public enum Route {
        ADD_PROPERTY,
        UNKNOWN
    }
}