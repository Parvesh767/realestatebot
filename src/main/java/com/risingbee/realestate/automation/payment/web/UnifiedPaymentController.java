package com.risingbee.realestate.automation.payment.web;

import com.risingbee.realestate.automation.payment.service.UnifiedPaymentService;
import com.risingbee.realestate.automation.rent.domain.RentSchedule;
import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/unified-payments")
@RequiredArgsConstructor
public class UnifiedPaymentController {

    private final UnifiedPaymentService paymentService;
    private final RentScheduleRepository rentRepository;
    private final TenancyContractRepository contractRepository;

    /**
     * API endpoint to dynamically generate a standardized UPI payment payload & intent link for any bill/schedule.
     */
    @GetMapping("/upi-intent/{scheduleId}")
    public ResponseEntity<?> getUpiIntentData(@PathVariable Long scheduleId) {
        RentSchedule schedule = rentRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        TenancyContract contract = contractRepository.findById(schedule.getContract().getId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));

        String brokerUpi = paymentService.resolveActiveUpi(contract.getId());
        long amountDue = schedule.getTotalDueInr() - schedule.getPaidAmountInr();

        // Construct standard UPI deep link using the broker's active dynamic UPI
        String upiDeepLink = String.format("upi://pay?pa=%s&pn=PropertyHub&am=%d&cu=INR&tn=Rent_%s",
                brokerUpi, amountDue, schedule.getBillingCycleMonth().replaceAll("\\s+", "_"));

        return ResponseEntity.ok(Map.of(
                "upiId", brokerUpi,
                "amount", amountDue,
                "upiIntentLink", upiDeepLink
        ));
    }

    /**
     * Web/API endpoint to confirm and record a completed payment transaction (gateway webhook or verified manual screenshot).
     */
    @PostMapping("/verify-and-settle/{scheduleId}")
    public ResponseEntity<?> verifyAndSettlePayment(
            @PathVariable Long scheduleId,
            @RequestBody Map<String, Object> payload
    ) {
        String paymentMode = (String) payload.getOrDefault("paymentMode", "UPI");
        String txnRef = (String) payload.getOrDefault("transactionReference", "TXN_" + System.currentTimeMillis());
        Long amount = payload.containsKey("amount") ? Long.valueOf(payload.get("amount").toString()) : null;

        RentSchedule settledSchedule = paymentService.processSuccessfulPayment(scheduleId, paymentMode, txnRef, amount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "scheduleId", settledSchedule.getId(),
                "status", settledSchedule.getStatus().name()
        ));
    }
}