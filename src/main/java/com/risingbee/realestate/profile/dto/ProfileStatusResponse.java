package com.risingbee.realestate.profile.dto;

import com.risingbee.realestate.automation.actor.enums.ActorType;

public record ProfileStatusResponse(
	    Long accountId,
	    String phone,
	    ActorType role,
	    String profileStage,
	    String nextStep,
	    boolean profileCompleted
	) {}
