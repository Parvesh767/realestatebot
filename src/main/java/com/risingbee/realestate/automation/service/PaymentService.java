package com.risingbee.realestate.automation.service;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;

    @Value("${razorpay.key-id:mock_key}")
    private String keyId;

    @Value("${razorpay.key-secret:mock_secret}")
    private String keySecret;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Generates a dynamic Razorpay Payment Link for credit recharges.
     */
    public String createRechargePaymentLink(Long accountId, String brokerPhone, int amountInInr) {
        if (accountId == null) {
            log.error("Cannot create payment link: accountId is null");
            return baseUrl + "/dashboard.html";
        }

        try {
            // In dev / mock mode, fulfill recharge immediately and redirect
            if (keyId == null || keyId.startsWith("mock") || keySecret == null || keySecret.startsWith("mock")) {
                log.warn("[DEV MODE] Razorpay keys not configured. Simulating instant credit recharge for accountId={}", accountId);
                fulfillRecharge(accountId, amountInInr);
                return baseUrl + "/dashboard.html?recharge=success";
            }

            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject paymentLinkRequest = new JSONObject();
            // Razorpay accepts amount in paise (₹999 -> 99900 paise)
            paymentLinkRequest.put("amount", (long) amountInInr * 100);
            paymentLinkRequest.put("currency", "INR");
            paymentLinkRequest.put("accept_partial", false);
            paymentLinkRequest.put("description", "Real Estate Lead Credits Top-up (" + (amountInInr >= 2499 ? "100" : "30") + " Credits)");

            // Customer details
            JSONObject customer = new JSONObject();
            if (brokerPhone != null && !brokerPhone.isBlank()) {
                customer.put("contact", brokerPhone);
            }
            paymentLinkRequest.put("customer", customer);

            // Metadata: Passed to callback
            JSONObject notes = new JSONObject();
            notes.put("account_id", String.valueOf(accountId));
            notes.put("amount_inr", String.valueOf(amountInInr));
            notes.put("package", amountInInr >= 2499 ? "100_CREDITS" : "30_CREDITS");
            paymentLinkRequest.put("notes", notes);

            // Callback configuration
            paymentLinkRequest.put("callback_url", baseUrl + "/payment/callback");
            paymentLinkRequest.put("callback_method", "get");

            PaymentLink paymentLink = razorpay.paymentLink.create(paymentLinkRequest);
            String shortUrl = paymentLink.get("short_url");

            log.info("Created Razorpay Payment Link for accountId={}: url={}", accountId, shortUrl);
            return shortUrl;

        } catch (RazorpayException e) {
            log.error("Failed to generate Razorpay Payment Link for accountId={}, error={}", accountId, e.getMessage(), e);
            return baseUrl + "/dashboard.html?error=payment_failed";
        } catch (Exception e) {
            log.error("Unexpected error creating payment link for accountId={}", accountId, e);
            return baseUrl + "/dashboard.html?error=unexpected";
        }
    }

    /**
     * Credits the account balance upon successful payment.
     */
    @Transactional
    public void fulfillRecharge(Long accountId, int amountInInr) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        int creditsToAdd = calculateCreditsForAmount(amountInInr);
        int currentBalance = account.getCreditsBalance() != null ? account.getCreditsBalance() : 0;
        
        account.setCreditsBalance(currentBalance + creditsToAdd);
        accountRepository.save(account);

        log.info("Recharged {} credits for Account #{} (Phone: {}). New Balance: {}",
                creditsToAdd, accountId, account.getPhone(), account.getCreditsBalance());
    }
    
    
    /**
     * Fetches payment link details from Razorpay to retrieve original notes and amount.
     */
    public int getAmountFromPaymentLink(String paymentLinkId) {
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            PaymentLink link = razorpay.paymentLink.fetch(paymentLinkId);
            
            // Check notes first
            JSONObject notes = link.get("notes");
            if (notes != null && notes.has("amount_inr")) {
                return Integer.parseInt(notes.getString("amount_inr"));
            }

            // Fallback to link amount (paise -> INR)
            long amountPaise = link.get("amount");
            return (int) (amountPaise / 100);
        } catch (Exception e) {
            log.error("Failed to fetch Razorpay payment link {}: {}", paymentLinkId, e.getMessage());
            // Fallback threshold check if retrieval fails
            return 999;
        }
    }

    /**
     * Maps recharge fiat amount to platform credit bundles.
     */
    public int calculateCreditsForAmount(int amountInInr) {
        if (amountInInr >= 2499) return 100; // Pro Pack
        if (amountInInr >= 999) return 30;   // Starter Pack
        return Math.max(1, amountInInr / 33); // Custom pack fallback (~₹33/credit)
    }
}