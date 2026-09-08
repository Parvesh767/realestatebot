package com.risingbee.realestate.auth;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/web/auth")
@RequiredArgsConstructor
@Slf4j
public class WebAuthController {

    private final AccountRepository accountRepository;

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("ACTOR") != null) {
            return "redirect:/dashboard.html";
        }
        if (ActorContext.get() != null) {
            return "redirect:/dashboard.html";
        }
        return "auth/login";
    }

    @GetMapping("/send-otp")
    public String getSendOtpFallback() {
        return "redirect:/web/auth/login";
    }

    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String phone, Model model) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        log.info("Sending OTP challenge to: {}", cleanPhone);
        model.addAttribute("phone", cleanPhone);
        return "auth/verify";
    }

    @GetMapping("/verify")
    public String getVerifyFallback() {
        return "redirect:/web/auth/login";
    }

    @PostMapping("/verify")
    public String verifyOtp(
            @RequestParam String phone,
            @RequestParam String otp,
            HttpServletRequest request,
            Model model) {

        String cleanPhone = phone.replaceAll("[^0-9]", "");

        if (!"123456".equals(otp) && !"000000".equals(otp)) {
            model.addAttribute("phone", cleanPhone);
            model.addAttribute("error", "Invalid verification code. Please try again.");
            return "auth/verify";
        }

        Account account = accountRepository.findByExternalId(cleanPhone)
                .orElseGet(() -> {
                    Account newAcc = new Account();
                    newAcc.setExternalId(cleanPhone);
                    newAcc.setName("Broker " + cleanPhone.substring(Math.max(0, cleanPhone.length() - 4)));
                    newAcc.setType(ActorType.BROKER);
                    newAcc.setCreditsBalance(10);
                    newAcc.setActive(true);
                    return accountRepository.save(newAcc);
                });

        ActorType role = account.getType() != null ? account.getType() : ActorType.BROKER;
        Actor actor = new Actor(role, account.getExternalId(), account.getId());

        HttpSession session = request.getSession(true);
        session.setAttribute("ACTOR", actor);
        session.setAttribute("ACCOUNT_ID", account.getId());

        log.info("User authenticated -> Account ID #{}, Role: {}", account.getId(), account.getType());
        
     // Role-based routing
        if (account.getType() == ActorType.TENANT || account.getType() == ActorType.USER) {
            return "redirect:/tenant/portal";
        
        }
        return "redirect:/dashboard.html";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/web/auth/login";
    }
}