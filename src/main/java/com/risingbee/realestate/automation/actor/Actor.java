package com.risingbee.realestate.automation.actor;

import com.risingbee.realestate.automation.actor.enums.ActorType;

public record Actor(
        ActorType type,
        String externalId,   // phone number for WhatsApp
        Long internalId      // brokerId if broker, else null
) {}