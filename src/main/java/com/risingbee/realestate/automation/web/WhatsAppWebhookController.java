package com.risingbee.realestate.automation.web;


import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.service.WhatsAppService;
import com.risingbee.realestate.automation.tenant.BrokerContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j                         // Lombok: adds a Logger → log.info(), log.error(), etc.
@RestController               // Spring: marks class as REST endpoint handler
@RequestMapping("/api/whatsapp") // Base URL for all WhatsApp endpoints
@RequiredArgsConstructor      // Lombok: generates constructor for final fields (DI)
public class WhatsAppWebhookController {

	
    private final WhatsAppService whatsAppService;
	
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
    	
    	log.info("Broker resolved in context = {}", BrokerContext.get());
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
    public ResponseEntity<String> receiveMessage(@RequestBody Map<String, Object> payload) {
    	
    	log.info("Broker resolved in context = {}", BrokerContext.get());
        log.debug("Incoming WhatsApp Payload: {}", payload);

        whatsAppService.handleIncoming(payload);  // Delegation to service layer

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}