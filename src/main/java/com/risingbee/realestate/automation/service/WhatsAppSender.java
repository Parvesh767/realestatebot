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

    private final WebClient webClient;

    public void sendTextMessage(String to, String message) {

        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to", to,
            "type", "text",
            "text", Map.of("body", message)
        );

        webClient.post()
            .uri("https://graph.facebook.com/v20.0/{id}/messages", phoneNumberId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(resp ->
                log.debug(
                    "WhatsApp message sent. to={}, status={}",
                    to, resp.getStatusCode()
                )
            )
            .doOnError(err ->
                log.error(
                    "WhatsApp message failed. to={}",
                    to,
                    err
                )
            )
            .subscribe(); // fire-and-forget, explicitly
    }
}
