package com.risingbee.realestate.automation.actor;

import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.enums.ActorType;


public record Actor(
        ActorType role,
        String externalId,   // phone number for WhatsApp
        Long internalId      // brokerId if broker, else null
) {
	
	public Actor withAccount(Account account) {
	    return new Actor(
	        account.getType(),
	        this.externalId,
	        account.getId()
	    );
	}
	
}