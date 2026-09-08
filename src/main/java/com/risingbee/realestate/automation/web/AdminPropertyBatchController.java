package com.risingbee.realestate.automation.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.service.PropertyBatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequestMapping("/admin/properties/batch")
@RequiredArgsConstructor
public class AdminPropertyBatchController {

    private final PropertyBatchService batchService;

    // 1. Stream the CSV template to the browser
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        String templateContent = batchService.getCsvTemplate();
        byte[] output = templateContent.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"property_hub_bulk_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(output);
    }

    // 2. Handle the frontend file upload
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> uploadBatchData(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid CSV file."));
        }

        // Process the file using the service we wrote earlier
        PropertyBatchService.BatchResult result = batchService.processCsvUpload(actor, file);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "successCount", result.successCount(),
                "errors", result.errors()
        ));
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get(); // Relies on your existing ActorSessionFilter[cite: 1, 2]
        if (actor != null) return actor;
        
        HttpSession session = request.getSession(false);
        if (session != null) return (Actor) session.getAttribute("ACTOR");
        return null;
    }
}