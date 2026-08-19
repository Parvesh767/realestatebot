package com.risingbee.realestate.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.risingbee.realestate.auth.dto.AuthResponse;
import com.risingbee.realestate.auth.dto.VerifyOtpRequest;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.domain.OtpVerification;
import com.risingbee.realestate.automation.repo.OtpVerificationRepository;
import com.risingbee.realestate.profile.dto.ProfileStage;
import com.risingbee.realestate.profile.utility.ProfileStageResolver;
import com.risingbee.realestate.security.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

	private final OtpVerificationRepository otpRepository;
	private final AccountRepository accountRepository;
	
	private final JwtUtil jwtUtil;

	@Value("${app.otp.override-enabled}")
	private boolean otpOverrideEnabled;

	@Value("${app.otp.override-value}")
	private String otpOverrideValue;
	
	public AuthResponse verifyOtpAndHandleUser(VerifyOtpRequest request) {

	    validatePhone(request.phone());

	    // 1. Validate OTP
	    OtpVerification record = otpRepository
	        .findValidOtp(request.phone(), request.otp(), Instant.now())
	        .orElse(null);

	    if (!(otpOverrideEnabled && request.otp().equals(otpOverrideValue))) {

	        if (record == null) {
	            throw new IllegalArgumentException("Invalid or expired OTP");
	        }

	        if (!record.canBeUsed()) {
	            throw new IllegalStateException("OTP already used");
	        }
	    }

	    // 2. Find or create account
	    Account account = accountRepository
	        .findByExternalId(request.phone())
	        .orElseGet(() -> {
	            Account acc = new Account();
	            acc.setExternalId(request.phone());
	            acc.setVerified(true);
	            acc.setActive(true);
	            acc.setCreatedAt(LocalDateTime.now());

	            // DO NOT set role here (staging flow)
	            return accountRepository.save(acc);
	        });

	    // 3. Mark OTP used
	    if (record != null) {
	        record.markUsed();
	        otpRepository.save(record);
	    }

	    // 4. Resolve profile stage
	    ProfileStage stage = ProfileStageResolver.resolve(account);

	    // 5. Generate token
	    Actor actor = new Actor(
	        account.getType(),
	        account.getExternalId(),
	        account.getId()
	    );

	    String token = jwtUtil.generateToken(actor);

	    // 6. Return enriched response
	    return new AuthResponse(
	        token,
	        account.getId(),
	        account.getExternalId(),
	        account.getType(),
	        stage.name(),
	        ProfileStageResolver.nextStep(stage)
	    );
	}
	
	
	
	
	public void requestOtp(String phone) {

		validatePhone(phone);

//		// Ensure account exists (identity only)
//		accountRepository.findByExternalId(phone).orElseThrow(() -> new IllegalStateException("Account not found"));

		
		 // Optional: check existence for logging/analytics
	    boolean exists = accountRepository.findByExternalId(phone).isPresent();
	    log.debug("account present  : ", exists);

		otpRepository.invalidateActiveOtps(phone);

		String otp = generateOtp();

		otpRepository.save(new OtpVerification(phone, otp, Instant.now().plusSeconds(300)));

		log.warn("LOGIN OTP for {} is {}", phone, otp);
	}
	
	
	
	
	private void validatePhone(String phone) {
		if (phone == null || !phone.matches("^91[0-9]{10}$")) {
			throw new IllegalArgumentException("Invalid phone number");
		}
	}
	
	


	private String generateOtp() {
		
		
		   if (otpOverrideEnabled) {
		        return otpOverrideValue;
		    }
		
		return String.valueOf(100000 + new SecureRandom().nextInt(900000));
	}
}
