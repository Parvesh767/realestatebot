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

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.parser.WhatsAppPayloadExtractor;
import com.risingbee.realestate.automation.repo.BrokerRepository;
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
    private final WhatsAppPayloadExtractor payloadExtractor;
    private final BrokerRepository brokerRepository;
	
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

        try {
            // 1. Extract sender phone from payload
            Optional<String> phoneOpt = payloadExtractor.extractPhone(payload);

            if (phoneOpt.isEmpty()) {
                log.debug("Ignoring non-message webhook event (status/update)");
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            String phone = phoneOpt.get();
            log.info("Incoming WhatsApp message from phone = {}", phone);

            // 2. Resolve broker by phone
            Optional<Broker> brokerOpt = brokerRepository.findByPhone(phone);

            if (brokerOpt.isEmpty()) {
                log.warn("No broker found for phone = {}", phone);
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            // 3. Set broker into context BEFORE service call
            BrokerContext.set(brokerOpt.get());
            log.info("Broker resolved in context = {}", brokerOpt.get().getName());

            // 4. Delegate to service
            whatsAppService.handleIncoming(payload);

        } catch (Exception e) {
            log.error("Error handling WhatsApp webhook", e);
        } finally {
            // 5. ALWAYS clear context
            BrokerContext.clear();
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}