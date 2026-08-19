package com.risingbee.realestate.automation.actor;

import java.util.Map;
import java.util.Set;

import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.actor.enums.Capability;

public final class ActorCapabilities {

    private static final Map<ActorType, Set<Capability>> POLICY = Map.of(
//        ActorType.USER, Set.of(
//            Capability.SEARCH_PROPERTY,
//            Capability.BROKER_ONBOARDING
//
//        ),
        ActorType.OWNER, Set.of(
            Capability.SEARCH_PROPERTY,
            Capability.ADD_PROPERTY,
            Capability.BROKER_ONBOARDING
        ),
        ActorType.BROKER, Set.of(
            Capability.SEARCH_PROPERTY,
            Capability.ADD_PROPERTY,
            Capability.BROKER_ONBOARDING
        ),
        ActorType.ADMIN, Set.of(
            Capability.SEARCH_PROPERTY,
            Capability.ADD_PROPERTY,
            Capability.BROKER_ONBOARDING
        )
    );

    private ActorCapabilities() {}

    public static boolean allows(Actor actor, Capability capability) {
        return allows(actor.role(), capability);
    }

    public static boolean allows(ActorType type, Capability capability) {
        return POLICY.getOrDefault(type, Set.of()).contains(capability);
    }
}