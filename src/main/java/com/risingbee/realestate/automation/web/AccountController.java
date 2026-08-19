package com.risingbee.realestate.automation.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.actor.service.AccountService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable Long id) {
        log.info("Fetching balance for accountId={}", id);

        return accountService.findById(id)
                .map(account -> ResponseEntity.ok(Map.<String, Object>of(
                        "accountId", account.getId(),
                        "creditsBalance", account.getCreditsBalance() != null ? account.getCreditsBalance() : 0,
                        "role", account.getType() != null ? account.getType().name() : "BROKER"
                )))
                .orElseGet(() -> {
                    log.warn("Account not found for id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }
}