package com.risingbee.realestate.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.auth.dto.AuthResponse;
import com.risingbee.realestate.auth.dto.OtpResponse;
import com.risingbee.realestate.auth.dto.RequestOtpRequest;
import com.risingbee.realestate.auth.dto.VerifyOtpRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/request-otp")
    public ResponseEntity<OtpResponse> requestOtp(
            @RequestBody RequestOtpRequest request
    ) {
        authService.requestOtp(request.phone());
        return ResponseEntity.ok(new OtpResponse("OTP Sent Successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @RequestBody VerifyOtpRequest request
    ) {
        String token = authService.verifyOtp(
                request.phone(),
                request.otp()
        );
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
