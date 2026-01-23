package com.risingbee.realestate.auth;


import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.domain.OtpVerification;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.automation.repo.OtpVerificationRepository;
import com.risingbee.realestate.automation.service.BrokerService;
import com.risingbee.realestate.security.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final BrokerRepository brokerRepository;
    private final OtpVerificationRepository otpRepository;
    private final JwtUtil jwtService;
    private final BrokerService brokerService;

    public void requestOtp(String phone) {

        validatePhone(phone);

        
        
     // 🔍 Prefer ActorContext for identity (gentle read)
        Actor actor = ActorContext.get();

        Broker broker = null;

        if (actor != null && actor.type() == ActorType.BROKER) {
            broker = brokerRepository.findById(actor.internalId())
                    .orElse(null);

            if (broker == null) {
                log.warn(
                    "ActorContext says BROKER {} but broker not found in DB",
                    actor
                );
            }
        }

        // 🔁 Fallback to old behavior (unchanged semantics)
        if (broker == null) {
            broker = brokerRepository.findByPhone(phone)
                    .orElseGet(() -> brokerService.createForOnboarding(phone));
        }

        
        
//        // 1️⃣ Ensure broker exists
//        Broker broker = brokerRepository.findByPhone(phone)
//            .orElseGet(() -> brokerRepository.save(new Broker(phone)));

        // 2️⃣ Invalidate old OTPs (DB-level)
        otpRepository.invalidateActiveOtps(phone);

        // 3️⃣ Generate OTP
        String otp = generateOtp();

        OtpVerification record = new OtpVerification(
            phone,
            otp,
            Instant.now().plusSeconds(300)
        );

        otpRepository.save(record);

        // 4️⃣ Send OTP (TEMP: LOG)
        log.warn("LOGIN OTP for {} is {}", phone, otp);
    }

    public String verifyOtp(String phone, String otp) {

        validatePhone(phone);

        OtpVerification record = otpRepository
            .findValidOtp(phone, otp, Instant.now())
            .orElseThrow(() ->
                new IllegalArgumentException("Invalid or expired OTP")
            );

        // 1️⃣ Domain-enforced usage
        if (!record.canBeUsed()) {
            throw new IllegalStateException("OTP already used or expired");
        }

        record.markUsed();
        otpRepository.save(record);
        
        
     // 🔍 Prefer ActorContext for identity (gentle read)
        Actor actor = ActorContext.get();

        Broker broker = null;

        if (actor != null && actor.type() == ActorType.BROKER) {
            broker = brokerRepository.findById(actor.internalId())
                    .orElse(null);

            if (broker == null) {
                log.warn(
                    "ActorContext says BROKER {} but broker not found in DB",
                    actor
                );
            }
        }

        // 🔁 Fallback to old behavior (unchanged semantics)
        if (broker == null) {
            broker = brokerRepository.findByPhone(phone)
                    .orElseThrow(() -> new IllegalStateException("Broker not found"));
        }


        // 2️⃣ Fetch broker
//        Broker broker = brokerRepository
//            .findByPhone(phone)
//            .orElseThrow(() ->
//               
//            );

        // 3️⃣ Issue JWT
        return jwtService.generateToken(
            broker.getId(),
            broker.getPhone()
        );
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^91[0-9]{10}$")) {
            throw new IllegalArgumentException("Invalid phone number");
        }
    }

    private String generateOtp() {
        return String.valueOf(
            100000 + new SecureRandom().nextInt(900000)
        );
    }
}
