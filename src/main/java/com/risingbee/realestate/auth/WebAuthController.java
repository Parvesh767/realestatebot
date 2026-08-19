package com.risingbee.realestate.auth;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.risingbee.realestate.auth.dto.AuthResponse;
import com.risingbee.realestate.auth.dto.VerifyOtpRequest;
import com.risingbee.realestate.automation.actor.Actor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/web/auth")
public class WebAuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String phone, Model model) {

        authService.requestOtp(phone);

        model.addAttribute("phone", phone);
        return "auth/verify";
    }

    
 // Handles HTML Form Submissions (from Browser / Thymeleaf / UI)
    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String verifyOtpForm(VerifyOtpRequest request, HttpSession session) {
        return processVerification(request, session);
    }

    // Handles JSON API Requests (from React / Next.js / Postman)
    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String verifyOtpJson(@RequestBody VerifyOtpRequest request, HttpSession session) {
        return processVerification(request, session);
    }

    private String processVerification(VerifyOtpRequest request, HttpSession session) {
        AuthResponse authResponse = authService.verifyOtpAndHandleUser(request);
        Actor actor = new Actor(authResponse.role(), authResponse.phone(), authResponse.accountId());

        session.setAttribute("ACTOR", actor);
        session.setAttribute("ACCOUNT_ID", actor.internalId());

        return "redirect:/admin/properties";
    }
//    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
//    public String verifyOtp(
//    	 VerifyOtpRequest request,
//            HttpSession session
//    ) {
//
//    	
//    	
//    	 AuthResponse authResponse = authService.verifyOtpAndHandleUser(request);
//    	 
//    	 Actor actor = new Actor(authResponse.role() , authResponse.phone() , authResponse.accountId());
//     
//    	 
//    	 log.debug("actor at verify endpoint" , actor);
//
//        session.setAttribute("ACTOR", actor);
//        
//        
//        session.setAttribute("ACCOUNT_ID", actor.internalId());
//
//        return "redirect:/admin/properties";
//    }
}