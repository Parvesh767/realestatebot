package com.risingbee.realestate.automation.domain;

import java.time.Instant;

import com.risingbee.realestate.auth.dto.OtpPurpose;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "otp_verification")
@Getter
public class OtpVerification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String phoneNumber;
	private String otp;
	private Instant expiresAt;
	private boolean used = false;
	OtpPurpose purpose;

	/*
	 * ----------------- JPA requirement -----------------
	 */
	protected OtpVerification() {
	}

	/*
	 * ----------------- Domain construction -----------------
	 */
	public OtpVerification(String phoneNumber, String otp, Instant expiresAt) {
		this.phoneNumber = phoneNumber;
		this.otp = otp;
		this.expiresAt = expiresAt;
		this.used = false;
//		this.purpose = purpose;
	}

	/*
	 * ----------------- Domain logic -----------------
	 */

	public boolean isExpired() {
		return Instant.now().isAfter(expiresAt);
	}

	public boolean canBeUsed() {
		return !used && !isExpired();
	}

	public void markUsed() {
		if (used) {
			throw new IllegalStateException("OTP already used");
		}
		this.used = true;
	}
}
