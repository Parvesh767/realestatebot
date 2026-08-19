package com.risingbee.realestate.auth.dto;

public record RegisterVerifyRequest(
	    String phone,
	    String otp,
	    String name,
	    String email
	) {}
