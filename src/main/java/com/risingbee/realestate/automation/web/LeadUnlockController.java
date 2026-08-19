package com.risingbee.realestate.automation.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.dto.LeadResponseDTO;
import com.risingbee.realestate.automation.service.LeadPaywallService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadUnlockController {

    private final LeadPaywallService leadPaywallService;

    /**
     * Unlock a lead by deducting 1 credit from the broker's account.
     * Returns full unmasked buyer contact information.
     */
    @PostMapping("/{leadId}/unlock")
    public ResponseEntity<LeadResponseDTO> unlockLead(@PathVariable Long leadId) {
        Actor actor = ActorContext.get();
        LeadResponseDTO unlockedLead = leadPaywallService.unlockLeadForBroker(actor, leadId);
        return ResponseEntity.ok(unlockedLead);
    }
}