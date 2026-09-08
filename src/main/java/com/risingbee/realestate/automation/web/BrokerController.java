package com.risingbee.realestate.automation.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.service.BrokerService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
//@RequestMapping("/api/broker")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;

//    @GetMapping("/me")
//    public BrokerResponse me() {
//        Actor actor = ActorContext.get();
//        return brokerService.getMyProfile(actor);
//    }
    
    
    @GetMapping("/admin/profile")
    public String showProfilePage(Model model, HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor == null) {
            return "redirect:/web/auth/login";
        }
        return "admin/profile";
    }
}