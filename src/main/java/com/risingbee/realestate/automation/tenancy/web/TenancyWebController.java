package com.risingbee.realestate.automation.tenancy.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.rent.domain.RentPaymentStatus;
import com.risingbee.realestate.automation.rent.domain.RentSchedule;
import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
import com.risingbee.realestate.automation.repo.PropertyRepository;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.dto.DeductionRequestDTO;
import com.risingbee.realestate.automation.tenancy.dto.SettlementSummaryDTO;
import com.risingbee.realestate.automation.tenancy.dto.TenancyOverviewDTO;
import com.risingbee.realestate.automation.tenancy.enums.InspectionType;
import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
import com.risingbee.realestate.automation.tenancy.service.TenancyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/tenancy")
@RequiredArgsConstructor
public class TenancyWebController {

    private final TenancyService tenancyService;
    private final TenancyContractRepository contractRepository;
    private final PropertyRepository propertyRepository;
    private final RentScheduleRepository rentRepository;

    /**
     * 1. Portfolio Tenancies & Rent Overview Hub (Scoped to Current Broker)
     */
    @GetMapping
    public String viewTenancyDashboard(Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        // Isolated to properties and contracts owned by current broker/owner
        List<Property> brokerProperties = propertyRepository.findByOwnerAccountIdOrderByCreatedAtDesc(actor.internalId());
        List<TenancyContract> brokerContracts = contractRepository.findByOwnerAccountId(actor.internalId());

        Map<Long, Property> propertyMap = brokerProperties.stream()
                .collect(Collectors.toMap(Property::getId, Function.identity(), (a, b) -> a));

        String currentCycle = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        List<TenancyOverviewDTO> rentedList = new ArrayList<>();
        long totalActiveRent = 0L;
        long totalArrearsAll = 0L;
        int activeCount = 0;

        for (TenancyContract c : brokerContracts) {
            if (c.getStatus() != TenancyStatus.SETTLED) {
                Property p = propertyMap.get(c.getPropertyId());
                RentSchedule latestRent = rentRepository.findByContractIdAndBillingCycleMonth(c.getId(), currentCycle).orElse(null);
                Long arrears = rentRepository.getPendingArrearsByContract(c.getId());
                long pendingAmount = (arrears != null) ? arrears : 0L;

                RentPaymentStatus rentStatus = (latestRent != null) ? latestRent.getStatus() : RentPaymentStatus.PENDING;
                Long totalDue = (latestRent != null) ? latestRent.getTotalDueInr() : (c.getMonthlyRent() != null ? c.getMonthlyRent() : 0L);

                rentedList.add(TenancyOverviewDTO.builder()
                        .contractId(c.getId())
                        .propertyId(c.getPropertyId())
                        .propertyTitle(p != null && p.getTitle() != null ? p.getTitle() : "Property #" + c.getPropertyId())
                        .bhk(p != null ? p.getBhk() : "2BHK")
                        .locality(p != null && p.getLocalityCode() != null ? p.getLocalityCode() : "Gurgaon")
                        .tenantName(c.getTenantName() != null ? c.getTenantName() : "Tenant " + c.getTenantPhone())
                        .tenantPhone(c.getTenantPhone())
                        .monthlyRent(c.getMonthlyRent() != null ? c.getMonthlyRent() : 0L)
                        .securityDeposit(c.getSecurityDeposit() != null ? c.getSecurityDeposit() : 0L)
                        .startDate(c.getStartDate())
                        .endDate(c.getEndDate())
                        .tenancyStatus(c.getStatus())
                        .currentMonthCycle(currentCycle)
                        .currentTotalDue(totalDue)
                        .currentRentStatus(rentStatus)
                        .totalArrears(pendingAmount)
                        .build());

                totalActiveRent += (c.getMonthlyRent() != null ? c.getMonthlyRent() : 0L);
                totalArrearsAll += pendingAmount;
                activeCount++;
            }
        }

        int totalInventory = brokerProperties.size();
        int vacantCount = Math.max(0, totalInventory - activeCount);

        model.addAttribute("tenancies", rentedList);
        model.addAttribute("totalInventory", totalInventory);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("vacantCount", vacantCount);
        model.addAttribute("totalActiveRent", totalActiveRent);
        model.addAttribute("totalArrearsAll", totalArrearsAll);
        model.addAttribute("currentMonthCycle", currentCycle);

        return "tenancy/tenancies-overview";
    }

    /**
     * 2. View Monthly Rent & Utility Ledger for a Single Contract
     */
    @GetMapping("/{contractId}/rents")
    public String showRentLedger(@PathVariable Long contractId, Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        TenancyContract contract = validateContractAccess(contractId, actor);
        List<RentSchedule> schedules = rentRepository.findByContractIdOrderByDueDateDesc(contractId);
        Long pendingArrears = rentRepository.getPendingArrearsByContract(contractId);

        model.addAttribute("contract", contract);
        model.addAttribute("schedules", schedules);
        model.addAttribute("pendingArrears", (pendingArrears != null) ? pendingArrears : 0L);

        return "tenancy/rent-ledger";
    }

    /**
     * 3. View Move-Out Deposit Settlement Breakdown
     */
    @GetMapping("/{contractId}/settlement")
    public String viewSettlement(@PathVariable Long contractId, Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        validateContractAccess(contractId, actor);
        SettlementSummaryDTO settlement = tenancyService.calculateSettlement(contractId);
        model.addAttribute("settlement", settlement);
        model.addAttribute("actor", actor);
        return "tenancy/settlement";
    }

    /**
     * 4. Submit a Move-Out Evidence Deduction
     */
    @PostMapping("/{contractId}/deductions")
    public String addDeduction(
            @PathVariable Long contractId,
            @ModelAttribute DeductionRequestDTO dto,
            @RequestParam(value = "proofPhoto", required = false) MultipartFile proofPhoto,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        validateContractAccess(contractId, actor);
        tenancyService.addDeduction(actor, contractId, dto, proofPhoto);
        return "redirect:/tenancy/" + contractId + "/settlement";
    }

    /**
     * 5a. Show Digital Inspection Recording Form
     */
    @GetMapping("/{contractId}/inspections/new")
    public String showInspectionForm(@PathVariable Long contractId, Model model, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        TenancyContract contract = validateContractAccess(contractId, actor);
        Property property = propertyRepository.findById(contract.getPropertyId()).orElse(null);

        model.addAttribute("contract", contract);
        model.addAttribute("property", property);
        return "tenancy/record-inspection";
    }

    /**
     * 5b. Record Baseline Move-In / Move-Out Inspection
     */
    @PostMapping("/{contractId}/inspections")
    public String recordInspection(
            @PathVariable Long contractId,
            @RequestParam("type") InspectionType type,
            @RequestParam(value = "meterReading", required = false) Double meterReading,
            @RequestParam(value = "meterPhoto", required = false) MultipartFile meterPhoto,
            @RequestParam(value = "roomPhotos", required = false) List<MultipartFile> roomPhotos,
            @RequestParam(value = "notes", required = false) String notes,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        validateContractAccess(contractId, actor);
        tenancyService.recordInspection(actor, contractId, type, meterReading, meterPhoto, roomPhotos, notes);
        return "redirect:/tenancy/" + contractId + "/settlement";
    }

    /**
     * 6. Finalize & Settle Deposit
     */
    @PostMapping("/{contractId}/settle")
    public String settleContract(@PathVariable Long contractId, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return "redirect:/web/auth/login";
        }

        validateContractAccess(contractId, actor);
        tenancyService.settleContract(actor, contractId);
        return "redirect:/tenancy/" + contractId + "/settlement";
    }

    private TenancyContract validateContractAccess(Long contractId, Actor actor) {
        TenancyContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Contract ID: " + contractId));

        if (contract.getOwnerAccountId() != null && !contract.getOwnerAccountId().equals(actor.internalId())) {
            log.warn("Unauthorized access attempt on Contract #{} by Actor #{}", contractId, actor.internalId());
            throw new AccessDeniedException("Access denied to requested contract.");
        }
        return contract;
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