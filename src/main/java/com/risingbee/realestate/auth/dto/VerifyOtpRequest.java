package com.risingbee.realestate.auth.dto;

import com.risingbee.realestate.automation.actor.enums.ActorType;

public record VerifyOtpRequest(
	    String phone,
	    String otp,
	    String name,  
	    String email,
	    ActorType role   
	) {}