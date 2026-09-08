package com.risingbee.realestate.automation.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.service.LeadService;
import com.risingbee.realestate.leads.domain.LeadActivity;
import com.risingbee.realestate.leads.enums.LeadStatus;
import com.risingbee.realestate.leads.service.LeadActivityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Slf4j
public class LeadController {

    private final LeadService leadService;
    private final LeadActivityService leadActivityService;
    
    @GetMapping
    public ResponseEntity<?> getMyLeads() {
        Actor actor = ActorContext.get();
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please log in first."));
        }
        return ResponseEntity.ok(leadService.findAllForActor(actor));
    }
    
    @GetMapping("/{leadId}/activities")
    public ResponseEntity<List<LeadActivity>> getLeadTimeline(@PathVariable Long leadId) {
        List<LeadActivity> activities = leadActivityService.getTimelineForLead(leadId);
        return ResponseEntity.ok(activities);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateLeadStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status field is required"));
        }

        LeadStatus statusEnum;
        try {
            statusEnum = LeadStatus.valueOf(newStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid status value: " + newStatus,
                "allowedValues", LeadStatus.values()
            ));
        }

        try {
            Lead updatedLead = leadService.updateLeadStatus(id, statusEnum);
            log.info("Lead id={} status updated to {}", id, statusEnum);
            return ResponseEntity.ok(Map.of("id", id, "status", updatedLead.getStatus().name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}