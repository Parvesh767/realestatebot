package com.risingbee.realestate.automation.tenancy.web;

import com.risingbee.realestate.automation.tenancy.service.TenancyPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tenancy")
@RequiredArgsConstructor
public class TenancyApiController {

    private final TenancyPaymentService paymentService;

    @PostMapping("/{contractId}/rents/{scheduleId}/remind")
    public ResponseEntity<?> sendReminder(
            @PathVariable Long contractId,
            @PathVariable Long scheduleId) {
        try {
            paymentService.sendPaymentReminder(contractId, scheduleId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Reminder sent successfully via WhatsApp"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}