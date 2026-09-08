package com.risingbee.realestate.automation.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.domain.BrokerUpiHandle;
import com.risingbee.realestate.automation.repo.BrokerUpiHandleRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/broker/upi")
@RequiredArgsConstructor
public class BrokerUpiController {

    private final BrokerUpiHandleRepository upiRepository;

    @GetMapping
    public ResponseEntity<List<BrokerUpiHandle>> getMyUpiHandles(HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(upiRepository.findByAccountId(actor.internalId()));
    }

    @PostMapping
    public ResponseEntity<?> addUpiHandle(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return ResponseEntity.status(401).build();

        String upiId = body.get("upiId");
        if (upiId == null || upiId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "UPI ID is required"));
        }

        List<BrokerUpiHandle> existing = upiRepository.findByAccountId(actor.internalId());
        boolean makeDefault = existing.isEmpty(); // Make default automatically if it's the first one

        BrokerUpiHandle handle = new BrokerUpiHandle();
        handle.setAccountId(actor.internalId());
        handle.setUpiId(upiId.trim());
        handle.setDefault(makeDefault);
        upiRepository.save(handle);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<?> setDefaultUpi(@PathVariable Long id, HttpServletRequest request) {
        Actor actor = resolveActor(request);
        if (actor == null) return ResponseEntity.status(401).build();

        List<BrokerUpiHandle> handles = upiRepository.findByAccountId(actor.internalId());
        for (BrokerUpiHandle h : handles) {
            h.setDefault(h.getId().equals(id));
            upiRepository.save(h);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUpi(@PathVariable Long id, HttpServletRequest request) {
    	Actor actor = resolveActor(request);
        if (actor == null) return ResponseEntity.status(401).build();

        BrokerUpiHandle handle = upiRepository.findById(id).orElseThrow();
        if (!handle.getAccountId().equals(actor.internalId())) {
            return ResponseEntity.status(403).build();
        }

        boolean wasDefault = handle.isDefault();
        upiRepository.delete(handle);

        // If deleted handle was default, pick another one to be default
        if (wasDefault) {
            List remaining = upiRepository.findByAccountId(actor.internalId());
            if (!remaining.isEmpty()) {
                BrokerUpiHandle next = (BrokerUpiHandle) remaining.get(0);
                next.setDefault(true);
                upiRepository.save(next);
            }
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
    
    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) return actor;

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) {
            return a;
        }
        return null;
    }
    
    // helper resolveActor method...
}