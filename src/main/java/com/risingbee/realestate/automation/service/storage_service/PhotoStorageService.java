package com.risingbee.realestate.automation.service.storage_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoStorageService {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    public List<String> saveAll(List<MultipartFile> files) {
        try {
            if (!Files.exists(UPLOAD_DIR)) {
                Files.createDirectories(UPLOAD_DIR);
            }

            List<String> paths = new ArrayList<>();

            for (MultipartFile file : files) {
                String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
                Path target = UPLOAD_DIR.resolve(filename);
                Files.copy(file.getInputStream(), target);
                paths.add("/uploads/" + filename);
            }

            return paths;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save images", e);
        }
    }
    
    
    public void delete(String path) {
        try {
            Path file = Paths.get("uploads")
                .resolve(Paths.get(path).getFileName());
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete photo: " + path, e);
        }
    }

}

