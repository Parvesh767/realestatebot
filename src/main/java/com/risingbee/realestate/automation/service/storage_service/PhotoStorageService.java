package com.risingbee.realestate.automation.service.storage_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoStorageService {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    /**
     * Saves a single file (e.g. meterPhoto, damage proof, single avatar).
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            ensureUploadDirectoryExists();

            String originalName = file.getOriginalFilename() != null 
                    ? Paths.get(file.getOriginalFilename()).getFileName().toString().replaceAll("\\s+", "_") 
                    : "file.jpg";

            String filename = UUID.randomUUID() + "-" + originalName;
            Path target = UPLOAD_DIR.resolve(filename);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save photo: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Saves multiple files batch (e.g. property room photos).
     */
    public List<String> saveAll(List<MultipartFile> files) {
        List<String> paths = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return paths;
        }

        try {
            ensureUploadDirectoryExists();

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    paths.add(save(file));
                }
            }

            return paths;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save images batch", e);
        }
    }

    /**
     * Deletes a file from the uploads directory given its path or URL.
     */
    public void delete(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        try {
            Path file = UPLOAD_DIR.resolve(Paths.get(path).getFileName());
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete photo: " + path, e);
        }
    }

    private void ensureUploadDirectoryExists() throws IOException {
        if (!Files.exists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }
    }
}