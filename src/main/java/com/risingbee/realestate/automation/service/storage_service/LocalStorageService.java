package com.risingbee.realestate.automation.service.storage_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class LocalStorageService implements StorageService {

    private static final Path ROOT = Paths.get("uploads");

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(ROOT);
    }

    @Override
    public String store(byte[] bytes, String mimeType) {

        String ext = mimeType.contains("jpeg") ? "jpg" : "png";
        String fileName = UUID.randomUUID() + "." + ext;

        Path path = ROOT.resolve(fileName);

        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        return "/uploads/" + fileName;
    }
}

