package com.risingbee.realestate.auth.dto;

public record OtpResponse(
	    String phone,
	    long expiresIn
	) {}
