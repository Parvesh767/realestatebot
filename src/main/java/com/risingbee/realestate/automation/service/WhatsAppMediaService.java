package com.risingbee.realestate.automation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.risingbee.realestate.automation.dto.MediaInput;
import com.risingbee.realestate.automation.dto.WhatsAppMediaMeta;
import com.risingbee.realestate.automation.service.storage_service.StorageService;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor 
public class WhatsAppMediaService {

    private final WebClient webClient;
    private final StorageService storageService;
    

    @Value("${whatsapp.access-token}")
    private String token;

    public String downloadAndStore(MediaInput media) {

        WhatsAppMediaMeta meta = getMediaMeta(media.mediaId());
        byte[] bytes = download(meta.url());

        return storageService.store(bytes, media.mimeType());
    }

    private WhatsAppMediaMeta getMediaMeta(String mediaId) {

        return webClient.get()
            .uri("https://graph.facebook.com/v20.0/{id}", mediaId)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(WhatsAppMediaMeta.class)
            .block();
    }

    private byte[] download(String url) {

        return webClient.get()
            .uri(url)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(byte[].class)
            .block();
    }
}

