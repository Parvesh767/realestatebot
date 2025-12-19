package com.risingbee.realestate.automation.tenant;

import com.risingbee.realestate.automation.domain.Broker;

public class BrokerContext {

    private static final ThreadLocal<Broker> CURRENT = new ThreadLocal<>();

    public static void set(Broker broker) {
        CURRENT.set(broker);
    }

    public static Broker get() {
        return CURRENT.get();
    }

    public static Long id() {
        return CURRENT.get().getId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
