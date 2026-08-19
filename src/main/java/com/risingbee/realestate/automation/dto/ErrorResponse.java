package com.risingbee.realestate.automation.dto;

public record ErrorResponse(
	    String errorCode,
	    String errorMessage,
	    String details
	) {}