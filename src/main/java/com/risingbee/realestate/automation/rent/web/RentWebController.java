//package com.risingbee.realestate.automation.rent.web;
//
//import com.risingbee.realestate.automation.actor.Actor;
//import com.risingbee.realestate.automation.actor.ActorContext;
//import com.risingbee.realestate.automation.rent.domain.RentSchedule;
//import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
//import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
//import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.util.List;
//
//@Controller
//@RequestMapping("/tenancy")
//@RequiredArgsConstructor
//public class RentWebController {
//
//    private final RentScheduleRepository rentRepository;
//    private final TenancyContractRepository contractRepository;
//
//    @GetMapping("/{contractId}/rents")
//    public String showRentLedger(@PathVariable Long contractId, Model model, HttpServletRequest request) {
//        Actor actor = resolveActor(request);
//        if (actor == null) {
//            return "redirect:/web/auth/login";
//        }
//
//        TenancyContract contract = contractRepository.findById(contractId)
//                .orElseThrow(() -> new IllegalArgumentException("Invalid Contract ID: " + contractId));
//
//        List<RentSchedule> schedules = rentRepository.findByContractIdOrderByDueDateDesc(contractId);
//        Long pendingArrears = rentRepository.getPendingArrearsByContract(contractId);
//
//        model.addAttribute("contract", contract);
//        model.addAttribute("schedules", schedules);
//        model.addAttribute("pendingArrears", pendingArrears != null ? pendingArrears : 0L);
//
//        return "tenancy/rent-ledger";
//    }
//
//    private Actor resolveActor(HttpServletRequest request) {
//        Actor actor = ActorContext.get();
//        if (actor != null && actor.internalId() != null) return actor;
//        HttpSession session = request.getSession(false);
//        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) return a;
//        return null;
//    }
//}
