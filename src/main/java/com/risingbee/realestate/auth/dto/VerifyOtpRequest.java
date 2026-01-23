package com.risingbee.realestate.auth.dto;

public record VerifyOtpRequest(
		String phone,
		String otp) {

}
