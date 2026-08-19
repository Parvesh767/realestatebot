package com.risingbee.realestate.auth.dto;

import com.risingbee.realestate.automation.actor.enums.ActorType;

public record AuthResponse(
	    String token,
	    Long accountId,
	    String phone,
	    ActorType role,
	    String profileStage,
	    String nextStep
	) {}
