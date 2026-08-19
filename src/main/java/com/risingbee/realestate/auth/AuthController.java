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
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.dto.ApiResponse;
import com.risingbee.realestate.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    

    private final AuthService authService;

    
    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> requestOtp(
            @RequestBody RequestOtpRequest request
    ) {
        authService.requestOtp(request.phone());

        return ResponseEntity.ok(
            new ApiResponse<>(
                true,
                "OTP sent successfully",
                "OTP_SENT",
                new OtpResponse(request.phone() , 3000),
                null
            )
        );
    }
    
    
    
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @RequestBody VerifyOtpRequest request
    ) {
        AuthResponse authResponse = authService.verifyOtpAndHandleUser(request);

        return ResponseEntity.ok(
            new ApiResponse<>(
                true,
                "Authentication successful",
                "AUTH_SUCCESS",
                authResponse,
                null
            )
        );
    }
 
}
