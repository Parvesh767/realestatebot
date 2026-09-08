package com.risingbee.realestate.automation.web;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.parser.WhatsAppPayloadExtractor;
import com.risingbee.realestate.automation.service.WhatsAppService;
import com.risingbee.realestate.automation.service_hub.service.ServiceHubService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j                         // Lombok: adds a Logger → log.info(), log.error(), etc.
@RestController               // Spring: marks class as REST endpoint handler
@RequestMapping("/api/whatsapp") // Base URL for all WhatsApp endpoints
@RequiredArgsConstructor      // Lombok: generates constructor for final fields (DI)
public class WhatsAppWebhookController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppPayloadExtractor extractor;
    private final ServiceHubService serviceHubService;
	
    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    // -----------------------------------------------
    // STEP 4.1 — Webhook Verification (GET)
    // -----------------------------------------------
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.challenge", required = false) String challenge,
            @RequestParam(name = "hub.verify_token", required = false) String token
    ) {
        log.info("Webhook verification request → mode={}, token={}", mode, token);

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verified successfully.");
            return ResponseEntity.ok(challenge);
        }

        log.warn("Webhook verification failed.");
        return ResponseEntity.status(403).body("Verification failed");
    }

    // -----------------------------------------------
    // STEP 4.2 — Receive WhatsApp Messages (POST)	
    // -----------------------------------------------
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveMessage(
            @RequestBody Map<String, Object> payload
    ) {
        log.info("Incoming WhatsApp webhook");

        try {
            if (!extractor.isUserMessageEvent(payload)) {
                return ResponseEntity.ok("IGNORED");
            }

            // Extract sender phone number and text message content safely
            Optional<String> phoneOpt = extractor.extractPhone(payload);
            Optional<String> textOpt = extractor.extractText(payload);

            if (phoneOpt.isPresent() && textOpt.isPresent()) {
                String fromPhone = phoneOpt.get();
                // Normalize text by replacing non-breaking spaces and trimming
                String cleanMessage = textOpt.get().replace('\u00A0', ' ').trim();
                String upperMessage = cleanMessage.toUpperCase();

                // ---------------------------------------------------------
                // 1️⃣ Intercept Technician Job Acceptance: "ACCEPT <BookingID>"
                // ---------------------------------------------------------
                if (upperMessage.startsWith("ACCEPT")) {
                    String[] parts = cleanMessage.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            Long bookingId = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
                            boolean claimed = serviceHubService.acceptJob(fromPhone, bookingId);
                            log.info("Webhook intercepted ACCEPT for Job #{}: success={}", bookingId, claimed);
                            return ResponseEntity.ok(claimed ? "JOB_CLAIMED" : "JOB_ALREADY_TAKEN");
                        } catch (Exception e) {
                            log.error("Error processing ACCEPT command via webhook: {}", e.getMessage(), e);
                        }
                    }
                }

                // ---------------------------------------------------------
                // 2️⃣ Intercept OTP Job Completion: "COMPLETE <BookingID> <OTP>"
                // ---------------------------------------------------------
                if (upperMessage.startsWith("COMPLETE")) {
                    String[] parts = cleanMessage.split("\\s+");
                    if (parts.length >= 3) {
                        try {
                            Long bookingId = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
                            String otp = parts[2].trim();
                            serviceHubService.completeJobWithOtp(bookingId, otp);
                            log.info("Webhook intercepted COMPLETE for Job #{} using OTP", bookingId);
                            return ResponseEntity.ok("JOB_COMPLETED");
                        } catch (Exception e) {
                            log.error("Error processing COMPLETE command via webhook: {}", e.getMessage(), e);
                        }
                    }
                }
            }

            // 3️⃣ Fall back to standard conversational chatbot flow (Search/Onboarding)
            whatsAppService.handleIncoming(payload);

            return ResponseEntity.ok("EVENT_RECEIVED");

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.ok("EVENT_RECEIVED"); // prevent retries
        }
    }
}