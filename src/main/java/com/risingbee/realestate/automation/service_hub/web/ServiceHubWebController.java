package com.risingbee.realestate.automation.service_hub.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.service_hub.dto.TicketDispatchDTO;
import com.risingbee.realestate.automation.service_hub.repo.ServiceBookingRepository;
import com.risingbee.realestate.automation.service_hub.repo.ServiceCatalogRepository;
import com.risingbee.realestate.automation.tenancy.domain.MaintenanceTicket;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.repo.MaintenanceTicketRepository;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceHubWebController {

	private final ServiceCatalogRepository catalogRepository;
    private final ServiceBookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final MaintenanceTicketRepository ticketRepository;
    private final TenancyContractRepository contractRepository; // Injected
    private final PropertyRepository propertyRepository;       // Injected

    @GetMapping("/hub")
    public String showHub(Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        Account account = accountRepository.findById(actor.internalId()).orElse(null);
        List<MaintenanceTicket> rawTickets = ticketRepository.findAll();

//        List<TicketDispatchDTO> dispatchTickets = rawTickets.stream().map(t -> {
//            TenancyContract contract = contractRepository.findById(t.getTenancyContractId()).orElse(null);
//            Property property = (contract != null) ? propertyRepository.findById(contract.getPropertyId()).orElse(null) : null;
//
//            String fullAddress = "";
//            String locality = "GUR";
//            String tenantPhone = "";
//
//            if (property != null) {
//                fullAddress = (property.getTitle() != null ? property.getTitle() : "") + 
//                              (property.getLocalityCode() != null ? ", " + property.getLocalityCode() : "");
//                if (property.getLocalityCode() != null) {
//                    locality = property.getLocalityCode();
//                }
//            }
//            if (contract != null && contract.getTenantPhone() != null) {
//                tenantPhone = contract.getTenantPhone();
//            }
//
//            return TicketDispatchDTO.builder()
//                    .ticket(t)
//                    .propertyAddress(fullAddress)
//                    .localityCode(locality)
//                    .tenantPhone(tenantPhone)
//                    .contractId(t.getTenancyContractId())
//                    .category(t.getCategory() != null ? t.getCategory().name() : "OTHER")
//                    .build();
//        }).toList();

        
        List<TicketDispatchDTO> dispatchTickets = rawTickets.stream().map(t -> {
            TenancyContract contract = contractRepository.findById(t.getTenancyContractId()).orElse(null);
            Property property = (contract != null && contract.getPropertyId() != null)
                    ? propertyRepository.findById(contract.getPropertyId()).orElse(null)
                    : null;

            String fullAddress = "";
            String locality = "GUR";
            String tenantPhone = "";
            Long propertyId = null;

            if (property != null) {
                fullAddress = (property.getTitle() != null ? property.getTitle() : "") +
                              (property.getLocalityCode() != null ? ", " + property.getLocalityCode() : "");
                if (property.getLocalityCode() != null) {
                    locality = property.getLocalityCode();
                }
                propertyId = property.getId();
            }
            if (contract != null && contract.getTenantPhone() != null) {
                tenantPhone = contract.getTenantPhone();
            }

            return TicketDispatchDTO.builder()
                    .ticket(t)
                    .ticketId(t.getId())
                    .title(t.getTitle())
                    .description(t.getDescription())
                    .status(t.getStatus())
                    .createdAt(t.getCreatedAt())
                    .category(t.getCategory() != null ? t.getCategory().name() : "OTHER")
                    .propertyAddress(fullAddress)
                    .localityCode(locality)
                    .tenantPhone(tenantPhone)
                    .contractId(t.getTenancyContractId())
                    .propertyId(propertyId)
                    .build();
        }).toList();
        
        
        model.addAttribute("catalog", catalogRepository.findByActiveTrueOrderByCategoryAsc());
        model.addAttribute("myBookings", bookingRepository.findByAccountIdOrderByCreatedAtDesc(actor.internalId()));
        model.addAttribute("account", account);
        model.addAttribute("actor", actor);
        model.addAttribute("tenantTickets", dispatchTickets); // Pass the enriched DTO list

        return "services/hub";
    }

    
    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null) return actor;
        HttpSession session = request.getSession(false);
        if (session != null) return (Actor) session.getAttribute("ACTOR");
        return null;
    }
}