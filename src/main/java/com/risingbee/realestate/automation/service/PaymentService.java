package com.risingbee.realestate.automation.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {

    @Value("${razorpay.key-id:mock_key}")
    private String keyId;

    @Value("${razorpay.key-secret:mock_secret}")
    private String keySecret;

    @Value("${app.base-url:https://yourdomain.com}")
    private String baseUrl;

    /**
     * Generates a dynamic Razorpay Payment Link for credit recharges.
     * 
     * @param accountId   The internal Account primary key ID
     * @param brokerPhone The broker's WhatsApp phone number
     * @param amountInInr Amount to charge in INR (e.g., 999 for 30 credits)
     * @return Razorpay payment short URL or fallback link
     */
    public String createRechargePaymentLink(Long accountId, String brokerPhone, int amountInInr) {
        if (accountId == null) {
            log.error("Cannot create payment link: accountId is null");
            return baseUrl + "/recharge";
        }

        try {
            // Check if dummy/mock keys are present in dev
            if (keyId == null || keyId.startsWith("mock") || keySecret == null || keySecret.startsWith("mock")) {
                log.warn("[DEV MODE] Razorpay keys not configured. Returning fallback test recharge URL.");
                return baseUrl + "/recharge?account=" + accountId;
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

            // Metadata: Passed to the payment webhook upon completion
            JSONObject notes = new JSONObject();
            notes.put("account_id", String.valueOf(accountId));
            notes.put("package", amountInInr >= 2499 ? "100_CREDITS" : "30_CREDITS");
            paymentLinkRequest.put("notes", notes);

            // Callback configuration
            paymentLinkRequest.put("callback_url", baseUrl + "/payment/success");
            paymentLinkRequest.put("callback_method", "get");

            PaymentLink paymentLink = razorpay.paymentLink.create(paymentLinkRequest);
            String shortUrl = paymentLink.get("short_url");

            log.info("Created Razorpay Payment Link for accountId={}: url={}", accountId, shortUrl);
            return shortUrl;

        } catch (RazorpayException e) {
            log.error("Failed to generate Razorpay Payment Link for accountId={}, error={}", accountId, e.getMessage(), e);
            return baseUrl + "/recharge?account=" + accountId;
        } catch (Exception e) {
            log.error("Unexpected error creating payment link for accountId={}", accountId, e);
            return baseUrl + "/recharge?account=" + accountId;
        }
    }
}