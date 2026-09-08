package com.risingbee.realestate.automation.rent.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.rent.domain.RentSchedule;
import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
import com.risingbee.realestate.automation.rent.service.RentTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rents")
@RequiredArgsConstructor
public class RentApiController {

    private final RentTrackingService rentTrackingService;
    private final RentScheduleRepository rentRepository;

    @PostMapping("/contracts/{contractId}/generate")
    public ResponseEntity<?> generateRentInvoice(
            @PathVariable Long contractId,
            @RequestBody GenerateRentRequest req,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        RentSchedule schedule = rentTrackingService.generateMonthlyRentDue(
                contractId,
                req.getBillingCycleMonth(),
                req.getDueDate(),
                req.getElectricityInr(),
                req.getMaintenanceInr(),
                req.getWaterInr()
        );

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "rentScheduleId", schedule.getId(),
                "totalDue", schedule.getTotalDueInr(),
                "dueDate", schedule.getDueDate()
        ));
    }

    @PostMapping("/{scheduleId}/pay")
    public ResponseEntity<?> recordPayment(
            @PathVariable Long scheduleId,
            @RequestBody RecordPaymentRequest req,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        RentSchedule updated = rentTrackingService.recordRentPayment(
                scheduleId,
                req.getAmountPaid(),
                req.getPaymentMode(),
                req.getTransactionReference()
        );

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "paymentStatus", updated.getStatus().name(),
                "paidAmount", updated.getPaidAmountInr()
        ));
    }

    @GetMapping("/contracts/{contractId}")
    public ResponseEntity<List<RentSchedule>> getRentsByContract(@PathVariable Long contractId) {
        return ResponseEntity.ok(rentRepository.findByContractIdOrderByDueDateDesc(contractId));
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) return actor;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) return a;
        return null;
    }

    @Data
    public static class GenerateRentRequest {
        private String billingCycleMonth;
        private LocalDate dueDate;
        private Long electricityInr = 0L;
        private Long maintenanceInr = 0L;
        private Long waterInr = 0L;
    }

    @Data
    public static class RecordPaymentRequest {
        private Long amountPaid;
        private String paymentMode; // "UPI", "BANK_TRANSFER", "CASH"
        private String transactionReference;
    }
}