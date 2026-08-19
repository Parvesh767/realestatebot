package com.risingbee.realestate.automation.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final AccountService accountService;

    @PostMapping("/recharge")
    public ResponseEntity<Map<String, String>> initiateRecharge(
            @RequestParam int amount,
            @RequestParam Long accountId) {
        
        Account account = accountService.findById(accountId).orElse(null);
        String phone = (account != null && account.getExternalId() != null) ? account.getExternalId() : "";

        String paymentUrl = paymentService.createRechargePaymentLink(accountId, phone, amount);
        return ResponseEntity.ok(Map.of("url", paymentUrl));
    }
}	