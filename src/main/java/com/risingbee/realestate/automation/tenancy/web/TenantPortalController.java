package com.risingbee.realestate.automation.tenancy.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.rent.domain.RentSchedule;
import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.service_hub.domain.ServiceBooking;
import com.risingbee.realestate.automation.service_hub.enums.BookingStatus;
import com.risingbee.realestate.automation.service_hub.repo.ServiceBookingRepository;
import com.risingbee.realestate.automation.tenancy.domain.MaintenanceTicket;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.dto.MaintenanceTicketRequestDTO;
import com.risingbee.realestate.automation.tenancy.dto.SettlementSummaryDTO;
import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;
import com.risingbee.realestate.automation.tenancy.repo.MaintenanceTicketRepository;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
import com.risingbee.realestate.automation.tenancy.service.TenancyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/tenant/portal")
@RequiredArgsConstructor
public class TenantPortalController {

    private final TenancyContractRepository contractRepository;
    private final PropertyRepository propertyRepository;
    private final RentScheduleRepository rentRepository;
    private final TenancyService tenancyService;
    private final  MaintenanceTicketRepository ticketRepository;
    private final ServiceBookingRepository bookingRepository;
    
    @GetMapping
    public String showTenantPortal(Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return "redirect:/web/auth/login";

        List<TenancyContract> contracts = contractRepository.findByTenantAccountId(actor.internalId());
        TenancyContract activeContract = contracts.stream()
                .filter(c -> c.getStatus() == TenancyStatus.ACTIVE)
                .findFirst()
                .orElse(contracts.isEmpty() ? null : contracts.get(0));

        if (activeContract != null) {
            Property property = propertyRepository.findById(activeContract.getPropertyId()).orElse(null);
            String currentCycle = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            RentSchedule currentBill = rentRepository.findByContractIdAndBillingCycleMonth(activeContract.getId(), currentCycle).orElse(null);
            List<RentSchedule> history = rentRepository.findByContractIdOrderByDueDateDesc(activeContract.getId());
            Long arrears = rentRepository.getPendingArrearsByContract(activeContract.getId());
            SettlementSummaryDTO settlement = tenancyService.calculateSettlement(activeContract.getId());

            List<MaintenanceTicket> tickets = ticketRepository.findByTenancyContractIdOrderByCreatedAtDesc(activeContract.getId());
            
         // Inside TenantPortalController.java #showTenantPortal:

            List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.ASSIGNED);
            List<ServiceBooking> activeBookings = bookingRepository
                    .findByTenancyContractIdAndStatusInOrderByCreatedAtDesc(activeContract.getId(), activeStatuses);

            model.addAttribute("activeBookings", activeBookings);
            
            
            model.addAttribute("contract", activeContract);
            model.addAttribute("property", property);
            model.addAttribute("currentBill", currentBill);
            model.addAttribute("history", history);
            model.addAttribute("arrears", arrears != null ? arrears : 0L);
            model.addAttribute("settlement", settlement);
            model.addAttribute("tickets", tickets); 
        }

        model.addAttribute("actor", actor);
        return "tenant/portal";
    }

    
    @PostMapping("/tickets")
    public String createTicket(
            @RequestParam("contractId") Long contractId,
            @ModelAttribute MaintenanceTicketRequestDTO dto,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        tenancyService.createTicket(actor, contractId, dto, photo);
        return "redirect:/tenant/portal";
    }

//    private Actor resolveActor(HttpServletRequest request) {
//        Actor actor = ActorContext.get();
//        if (actor != null && actor.internalId() != null) return actor;
//
//        HttpSession session = request.getSession(false);
//        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) {
//            return a;
//        }
//        return null;
//    }
    
    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) return actor;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) return a;
        return null;
    }
}