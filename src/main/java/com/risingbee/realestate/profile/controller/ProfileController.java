	package com.risingbee.realestate.profile.controller;
	
	import org.springframework.http.ResponseEntity;
	import org.springframework.web.bind.annotation.GetMapping;
	import org.springframework.web.bind.annotation.PostMapping;
	import org.springframework.web.bind.annotation.RequestBody;
	import org.springframework.web.bind.annotation.RequestMapping;
	import org.springframework.web.bind.annotation.RestController;
	
	import com.risingbee.realestate.auth.dto.AuthResponse;
	import com.risingbee.realestate.automation.dto.ApiResponse;
	import com.risingbee.realestate.profile.dto.BasicInfoRequest;
	import com.risingbee.realestate.profile.dto.CompleteProfileRequest;
	import com.risingbee.realestate.profile.dto.ProfileStatusResponse;
	import com.risingbee.realestate.profile.dto.SetRoleRequest;
	import com.risingbee.realestate.profile.service.ProfileService;
	
	import jakarta.validation.Valid;
	import lombok.RequiredArgsConstructor;
	
	@RestController
	@RequestMapping("/api/profile")
	@RequiredArgsConstructor
	public class ProfileController {
	
	    private final ProfileService profileService;
	
	    // 1. Get current profile status (MOST IMPORTANT)
	    @GetMapping("/status")
	    public ResponseEntity<ApiResponse<ProfileStatusResponse>> getStatus() {
	        return ResponseEntity.ok(
	            ApiResponse.success(
	                "Profile status fetched",
	                "PROFILE_STATUS",
	                profileService.getStatus()
	            )
	        );
	    }
	
	    // 2. Set role (step 1)
	    @PostMapping("/role")
	    public ResponseEntity<ApiResponse<AuthResponse>> setRole(
	            @Valid @RequestBody SetRoleRequest request
	    ) {
	        AuthResponse res = profileService.setRoleAndRefreshToken(request);
	
	        return ResponseEntity.ok(
	            ApiResponse.success("Role set", "ROLE_SET", res)
	        );
	    }
	
	    // 3. Basic info (step 2)
	    @PostMapping("/basic")
	    public ResponseEntity<ApiResponse<Void>> updateBasic(
	            @RequestBody BasicInfoRequest request
	    ) {
	        profileService.updateBasicInfo(request);
	
	        return ResponseEntity.ok(
	            ApiResponse.success(
	                "Basic info updated",
	                "BASIC_INFO_UPDATED",
	                null
	            )
	        );
	    }
	
	    // 4. Complete profile (final step)
	    @PostMapping("/complete")
	    public ResponseEntity<ApiResponse<ProfileStatusResponse>> completeProfile(
	            @RequestBody CompleteProfileRequest request
	    ) {
	        ProfileStatusResponse response = profileService.completeProfile(request);
	
	        return ResponseEntity.ok(
	            ApiResponse.success(
	                "Profile completed successfully",
	                "PROFILE_COMPLETED",
	                response
	            )
	        );
	    }
	}
	
