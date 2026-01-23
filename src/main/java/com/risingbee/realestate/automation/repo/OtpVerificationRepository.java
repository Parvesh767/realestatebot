package com.risingbee.realestate.automation.repo;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.risingbee.realestate.automation.domain.OtpVerification;

public interface OtpVerificationRepository
extends JpaRepository<OtpVerification, Long> {

@Modifying
@Query("""
update OtpVerification o
set o.used = true
where o.phoneNumber = :phone
  and o.used = false
""")
void invalidateActiveOtps(@Param("phone") String phone);

@Query("""
select o from OtpVerification o
where o.phoneNumber = :phone
  and o.otp = :otp
  and o.used = false
  and o.expiresAt > :now
""")
Optional<OtpVerification> findValidOtp(
@Param("phone") String phone,
@Param("otp") String otp,
@Param("now") Instant now
);
}
