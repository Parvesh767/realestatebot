package com.risingbee.realestate.automation.web;


import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardWebController {

    @GetMapping({"/", "/dashboard", "/dashboard.html"})
    public String renderDashboard(HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }
        return "dashboard"; // resolves to src/main/resources/templates/dashboard.html
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) return actor;
        
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) {
            return a;
        }
        return null;
    }
}