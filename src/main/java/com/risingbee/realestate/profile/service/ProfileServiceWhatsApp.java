package com.risingbee.realestate.profile.service;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.auth.dto.AuthResponse;
import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.profile.dto.BasicInfoRequest;
import com.risingbee.realestate.profile.dto.CompleteProfileRequest;
import com.risingbee.realestate.profile.dto.ProfileStatusResponse;
import com.risingbee.realestate.profile.dto.SetRoleRequest;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class ProfileServiceWhatsApp {
	
	
	private final ProfileService profileService;
	private final WhatsAppSender send;
	
	
	
	public void handleProfileFlow(String message, ProfileStatusResponse status) {

	    switch (status.profileStage()) {

	        case "ROLE_PENDING":
	            handleRoleSelection(status.phone(), message);
	            break;

	        case "BASIC_INFO":
	            handleBasicInfo(status.phone(),message);
	            break;

	        case "ANONYMOUS":
	            send.sendTextMessage(status.phone(), "Please complete onboarding first");
	            break;

	        default:
	        	send.sendTextMessage(status.phone(),"Processing...");
	    }
	}
	
	public void handleRoleSelection(String phone , String message) {

	    ActorType role;

	    if ("1".equals(message)) {
	        role = ActorType.BROKER;
	    } else if ("2".equals(message)) {
	        role = ActorType.OWNER;
	    } else {
	    	send.sendTextMessage(phone,"Reply 1 for Broker, 2 for Owner");
	        return;
	    }

	    AuthResponse response =
	        profileService.setRoleAndRefreshToken(new SetRoleRequest(role));

	    // 🔥 VERY IMPORTANT

	    send.sendTextMessage(phone,"✅ Role set. Now enter your name and email.");
	}
	
	
	public void handleBasicInfo(String phone,String message) {

	    try {
	        String[] parts = message.split(",");

	        BasicInfoRequest req = new BasicInfoRequest(
	            parts[0].trim(),
	            parts[1].trim()
	        );

	        profileService.updateBasicInfo(req);

	        send.sendTextMessage(phone,"✅ Basic info saved. Now complete profile.");

	    } catch (Exception e) {
	    	send.sendTextMessage(phone,"❌ Format: Name, Email");
	    }
	}
	
	
	public void handleCompleteProfile(String phone ,String message, ActorType role) {

	    CompleteProfileRequest req;

	    if (role == ActorType.BROKER) {
	        req = new CompleteProfileRequest(
	            null, null, role,
	            message,   // agency name
	            null
	        );
	    } else {
	        req = new CompleteProfileRequest(
	            null, null, role,
	            null,
	            message // city
	        );
	    }

	    ProfileStatusResponse response =
	        profileService.completeProfile(req);

	    send.sendTextMessage(phone,"✅ Profile completed. You can now continue.");

	}

}
