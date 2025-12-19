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
import com.risingbee.realestate.automation.service.BrokerService;
import com.risingbee.realestate.automation.service.WhatsAppService;
import com.risingbee.realestate.automation.tenant.BrokerContext;
import com.risingbee.realestate.enums.BrokerOnboardingStep;
import com.risingbee.realestate.enums.BrokerStatus;

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
    private final BrokerService brokerService;
	
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

            
            Broker broker = brokerRepository
                    .findByPhone(phone)
                    .orElseGet(() -> {
                        log.info("Creating new broker for phone = {}", phone);

                        Broker b = new Broker();
                        b.setPhone(phone);
                        b.setStatus(BrokerStatus.ONBOARDING);
                        b.setOnboardingStep(BrokerOnboardingStep.START);
                        return brokerRepository.save(b);
                    });

            try {
                // 🔑 THIS IS THE MISSING LINE
                BrokerContext.set(broker);

                whatsAppService.handleIncoming(payload);

            } finally {
                // 🧹 CRITICAL: avoid thread leakage
                BrokerContext.clear();
            }

            return ResponseEntity.ok("EVENT_RECEIVED");
        }
}