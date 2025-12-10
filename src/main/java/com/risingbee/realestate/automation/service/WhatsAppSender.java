package com.risingbee.realestate.automation.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppSender {

    @Value("${whatsapp.access-token}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    private final WebClient webClient = WebClient.builder().build();

    // ---------------------------------------------------
    // METHOD 1: Send a simple text message
    // ---------------------------------------------------
    public void sendTextMessage(String to, String message) {

        String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", message)
        );

        log.info("Sending WhatsApp message to {} → {}", to, message);
        log.debug("Payload: {}", payload);

        webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("WhatsApp API Response: {}", res))
                .doOnError(err -> log.error("Error sending WhatsApp message", err))
                .subscribe(); // fire-and-forget
    }
}
