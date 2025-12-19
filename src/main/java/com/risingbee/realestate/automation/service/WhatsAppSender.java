package com.risingbee.realestate.automation.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of(
                        "body", message
                )
        );

        webClient.post()
                .uri("https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> log.info("WhatsApp API Response: {}", resp))
                .doOnError(err -> log.error("WhatsApp send failed", err))
                .subscribe();
    }
}
