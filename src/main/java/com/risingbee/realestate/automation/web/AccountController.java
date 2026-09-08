package com.risingbee.realestate.automation.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.actor.service.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;

    /**
     * Resolves currently authenticated broker profile & credit balance from ActorContext.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAccount() {
        Actor actor = ActorContext.get();
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthenticated session"));
        }

        
        
        return accountService.findById(actor.internalId())
                .map(account -> ResponseEntity.ok(Map.of(
                        "accountId", account.getId(),
                        "name", account.getName() != null ? account.getName() : "Broker",
                        "phone", account.getExternalId() != null ? account.getExternalId() : "",
                        "creditsBalance", account.getCreditsBalance() != null ? account.getCreditsBalance() : 0,
                        "role", account.getType() != null ? account.getType().name() : "BROKER",
                        	"upiId", account.getUpiId() != null ? account.getUpiId() : ""
                		
                		)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Context-aware balance check.
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getMyBalance() {
        Actor actor = ActorContext.get();
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return accountService.findById(actor.internalId())
                .map(account -> ResponseEntity.ok(Map.of(
                        "accountId", account.getId(),
                        "creditsBalance", account.getCreditsBalance() != null ? account.getCreditsBalance() : 0,
                        "role", account.getType() != null ? account.getType().name() : "BROKER"
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    
    
    @PostMapping("/profile/upi")
    public ResponseEntity<?> updateUpiId(@RequestBody Map<String, String> body, HttpServletRequest request) {
    	  Actor actor = ActorContext.get();
        if (actor == null) return ResponseEntity.status(401).build();

        String newUpi = body.get("upiId");
        Account account = accountRepository.findById(actor.internalId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setUpiId(newUpi);
        accountRepository.save(account);

        log.info("Broker account #{} updated preferred UPI ID to: {}", actor.internalId(), newUpi);
        return ResponseEntity.ok(Map.of("success", true, "upiId", newUpi));
    }
}