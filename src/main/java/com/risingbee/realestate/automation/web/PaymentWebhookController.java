package com.risingbee.realestate.automation.web;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.service.WhatsAppSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final AccountService accountService;
    private final WhatsAppSender whatsAppSender;

    
    
    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaymentWebhook(@RequestBody String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event");

            // 🟢 SCENARIO 1: PAYMENT SUCCESSFUL
            if ("payment_link.paid".equals(event) || "payment.captured".equals(event)) {
                JSONObject entity = extractEntity(json);
                JSONObject notes = entity.getJSONObject("notes");
                
                Long accountId = Long.parseLong(notes.getString("account_id"));
                int amountPaidInInr = entity.getInt("amount") / 100;
                int creditsToAdd = (amountPaidInInr >= 2499) ? 100 : 30;

                // Credit the account
                Account updatedAccount = accountService.addCreditsToAccount(accountId, creditsToAdd);

                // Send Confirmation
                sendWhatsAppSafe(
                    updatedAccount.getExternalId(),
                    """
                    🎉 *PAYMENT RECEIVED!*
                    
                    💰 *Amount:* ₹%d
                    💳 *Credits Added:* +%d
                    📊 *Current Balance:* %d Credits
                    
                    👉 Reply *UNLOCK* to reveal your buyer's phone number!
                    """.formatted(amountPaidInInr, creditsToAdd, updatedAccount.getCreditsBalance())
                );
            }

            // 🔴 SCENARIO 2: PAYMENT FAILED (Insufficient Balance, Bank Error, Cancelled)
            else if ("payment.failed".equals(event) || "payment_link.cancelled".equals(event)) {
                JSONObject entity = extractEntity(json);
                JSONObject notes = entity.optJSONObject("notes");

                if (notes != null && notes.has("account_id")) {
                    Long accountId = Long.parseLong(notes.getString("account_id"));
                    
                    // Fetch broker account
                    Account account = accountService.findById(accountId).orElse(null);

                    if (account != null && account.getExternalId() != null) {
                        // Extract failure reason from Razorpay payload if available
                        String errorReason = entity.optJSONObject("error") != null 
                                ? entity.getJSONObject("error").optString("description", "Transaction was declined")
                                : "Payment could not be completed";

                        sendWhatsAppSafe(
                            account.getExternalId(),
                            """
                            ❌ *PAYMENT FAILED!*
                            
                            Reason: %s
                            
                            Your account balance remains unchanged. You can try again using a different UPI ID or Card:
                            💳 *Retry Payment:* https://yourdomain.com/recharge?account=%d
                            """.formatted(errorReason, accountId)
                        );
                    }
                }
            }

            return ResponseEntity.ok("WEBHOOK_PROCESSED");

        } catch (Exception e) {
            log.error("Error processing payment webhook", e);
            return ResponseEntity.ok("EVENT_HANDLED_WITH_WARNINGS");
        }
    }

    private JSONObject extractEntity(JSONObject json) {
        if (json.has("payload") && json.getJSONObject("payload").has("payment_link")) {
            return json.getJSONObject("payload").getJSONObject("payment_link").getJSONObject("entity");
        }
        return json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
    }

    private void sendWhatsAppSafe(String phone, String message) {
        try {
            whatsAppSender.sendTextMessage(phone, message);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", phone, e.getMessage());
        }
    }
    
    
    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccessPage() {
        return ResponseEntity.ok("""
            <!DOCTYPE html>
            <html>
            <head><title>Payment Successful</title></head>
            <body style="font-family: Arial, sans-serif; text-align: center; padding-top: 50px;">
                <h1 style="color: #28a745;">🎉 Payment Successful!</h1>
                <p>Your lead credits have been added to your account.</p>
                <p>Please return to <strong>WhatsApp</strong> and reply <strong>UNLOCK</strong> to reveal your lead.</p>
            </body>
            </html>
        """);
    }
    
}