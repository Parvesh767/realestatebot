package com.risingbee.realestate.automation.tenant;

import com.risingbee.realestate.automation.domain.Broker;

/**
 * Simple request-scoped broker holder.
 * Always call clear() in finally/afterCompletion.
 */
public final class BrokerContext {

    private static final ThreadLocal<Broker> HOLDER = new ThreadLocal<>();

    private BrokerContext() {}

    public static void set(Broker broker) {
        HOLDER.set(broker);
    }

    public static Broker get() {
        return HOLDER.get();
    }

    public static Long id() {
        Broker b = HOLDER.get();
        return b == null ? null : b.getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
