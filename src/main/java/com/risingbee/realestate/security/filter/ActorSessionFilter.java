package com.risingbee.realestate.security.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActorSessionFilter extends OncePerRequestFilter {

    private final AccountRepository accountRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);

            if (session != null) {
                // 1. Fetch pre-constructed Actor from session to minimize DB hits
                Actor actor = (Actor) session.getAttribute("ACTOR");

                // 2. If not found in session, fallback to DB lookup via ACCOUNT_ID
                if (actor == null) {
                    Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
                    if (accountId != null) {
                        Account account = accountRepository.findById(accountId).orElse(null);

                        if (account != null) {
                            actor = new Actor(
                                    account.getType(), // Can be null during onboarding
                                    account.getExternalId(),
                                    account.getId()
                            );
                            session.setAttribute("ACTOR", actor);
                        }
                    }
                }

                // 3. Bind Actor and Security Authentication
                if (actor != null) {
                    String authorityName = (actor.role() != null)
                            ? "ROLE_" + actor.role().name()
                            : "ROLE_ONBOARDING";

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            actor.internalId(),
                            null,
                            List.of(new SimpleGrantedAuthority(authorityName))
                    );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    ActorContext.set(actor);

                    log.debug("Actor successfully bound with authority {}: {}", authorityName, actor);
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            // Clean up ThreadLocal contexts to prevent thread leakage
            SecurityContextHolder.clearContext();
            ActorContext.clear();
        }
    }
}