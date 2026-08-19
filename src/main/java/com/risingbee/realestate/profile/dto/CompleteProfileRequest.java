package com.risingbee.realestate.profile.dto;

import com.risingbee.realestate.automation.actor.enums.ActorType;

public record CompleteProfileRequest(
	    String name,
	    String email,
	    ActorType role,     // BROKER or OWNER

	    // broker-specific
	    String agencyName,

	    // owner-specific (optional for now)
	    String city
	) {}