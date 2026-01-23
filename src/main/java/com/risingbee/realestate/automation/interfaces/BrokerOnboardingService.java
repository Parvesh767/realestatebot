package com.risingbee.realestate.automation.interfaces;

import com.risingbee.realestate.automation.actor.Actor;

public interface BrokerOnboardingService {
    void handle(Actor actor, String message);
}
