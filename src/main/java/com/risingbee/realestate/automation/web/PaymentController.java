package com.risingbee.realestate.automation.web;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Endpoint invoked by dashboard.html modal
     */
    @PostMapping("/api/payments/recharge")
    @ResponseBody
    public ResponseEntity<?> initiateRecharge(
            @RequestParam("amount") int amount,
            HttpServletRequest request
    ) {
        Actor actor = resolveActor(request);
        if (actor == null || actor.internalId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        String paymentUrl = paymentService.createRechargePaymentLink(
                actor.internalId(),
                actor.externalId(),
                amount
        );

        return ResponseEntity.ok(Map.of("url", paymentUrl));
    }

    /**
     * Razorpay redirects customer back here after payment
     */
    @GetMapping("/payment/callback")
    public String handleRazorpayCallback(
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_payment_link_id") String paymentLinkId,
            @RequestParam("razorpay_payment_link_status") String status,
            HttpServletRequest request) {

        log.info("Payment Link {} marked {} (Payment ID: {})", paymentLinkId, status, paymentId);

        if ("paid".equalsIgnoreCase(status)) {
            // 1. Resolve logged in Actor/Account
            Actor actor = ActorContext.get();
            Long accountId = (actor != null) ? actor.internalId() : 10L;

            // 2. Fetch the true amount paid (₹2499 -> 100 credits, ₹999 -> 30 credits)
            int amountPaidInr = paymentService.getAmountFromPaymentLink(paymentLinkId);

            // 3. Fulfill recharge dynamically
            paymentService.fulfillRecharge(accountId, amountPaidInr);

            return "redirect:/dashboard.html?recharge=success";
        }

        return "redirect:/dashboard.html?error=payment_unsuccessful";
    }

    private Actor resolveActor(HttpServletRequest request) {
        Actor actor = ActorContext.get();
        if (actor != null && actor.internalId() != null) return actor;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ACTOR") instanceof Actor a) return a;
        return null;
    }
}