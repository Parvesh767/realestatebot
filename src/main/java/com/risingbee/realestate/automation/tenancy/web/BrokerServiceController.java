package com.risingbee.realestate.automation.tenancy.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.tenancy.domain.MaintenanceTicket;
import com.risingbee.realestate.automation.tenancy.enums.TicketStatus;
import com.risingbee.realestate.automation.tenancy.repo.MaintenanceTicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/services")
@RequiredArgsConstructor
public class BrokerServiceController {

    private final MaintenanceTicketRepository ticketRepository;

    @GetMapping
    public String listTickets(Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        List<MaintenanceTicket> tickets = ticketRepository.findAll();
        model.addAttribute("tickets", tickets);
        return "admin/services";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam TicketStatus status) {
        MaintenanceTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
        ticket.setStatus(status);
        ticketRepository.save(ticket);
        return "redirect:/admin/services";
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) return actor;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) return a;
        return null;
    }
}