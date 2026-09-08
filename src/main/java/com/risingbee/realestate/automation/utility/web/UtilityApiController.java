package com.risingbee.realestate.automation.utility.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.utility.domain.UtilityBill;
import com.risingbee.realestate.automation.utility.dto.CreateUtilityBillRequestDTO;
import com.risingbee.realestate.automation.utility.repo.UtilityBillRepository;
import com.risingbee.realestate.automation.utility.service.UtilityBillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utility")
@RequiredArgsConstructor
public class UtilityApiController {

    private final UtilityBillingService utilityBillingService;
    private final UtilityBillRepository utilityBillRepository;

    @PostMapping("/contracts/{contractId}/bills")
    public ResponseEntity<?> createBill(
            @PathVariable Long contractId,
            @RequestBody CreateUtilityBillRequestDTO dto,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Authentication required"));
        }

        UtilityBill bill = utilityBillingService.generateMonthlyBill(
                actor,
                contractId,
                dto.getBillingMonth(),
                dto.getPreviousReading(),
                dto.getCurrentReading(),
                dto.getRatePerUnit(),
                dto.getFixedMaintenance(),
                dto.getFixedWater()
        );

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "billId", bill.getId(),
                "billingMonth", bill.getBillingMonth(),
                "unitsConsumed", bill.getUnitsConsumed(),
                "electricityAmount", bill.getElectricityAmountInr(),
                "totalPayable", bill.getTotalPayableInr()
        ));
    }

    @GetMapping("/contracts/{contractId}/bills")
    public ResponseEntity<List<UtilityBill>> getBillsByContract(@PathVariable Long contractId) {
        return ResponseEntity.ok(utilityBillRepository.findByContractIdOrderByReadingDateDesc(contractId));
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) {
            return actor;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object sessionActor = session.getAttribute("ACTOR");
            if (sessionActor instanceof Actor a) {
                return a;
            }
        }
        return null;
    }
}