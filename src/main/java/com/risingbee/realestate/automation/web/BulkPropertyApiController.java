package com.risingbee.realestate.automation.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.dto.PropertyRequestDTO;
import com.risingbee.realestate.automation.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class BulkPropertyApiController {

    private final PropertyService propertyService;

    @PostMapping("/batch")
    public ResponseEntity<?> syncBulkProperties(
            @RequestBody List<PropertyRequestDTO> payload,
            HttpServletRequest request) {
        
        Actor actor = extractActorOrThrow(request); // Assume standard auth resolution
        int success = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < payload.size(); i++) {
            try {
                // Ingest multiple properties programmatically[cite: 1]
                propertyService.create(actor, payload.get(i), null);
                success++;
            } catch (Exception e) {
                errors.add("Index " + i + " failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
            "status", "COMPLETED",
            "totalReceived", payload.size(),
            "successfullySynced", success,
            "errors", errors
        ));
    }
    
    private Actor extractActorOrThrow(HttpServletRequest request) {
        // Implement your standard JWT / Bearer token extraction here
        return (Actor) request.getAttribute("ACTOR"); 
    }
}