package com.risingbee.realestate.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Actor actor = jwtUtil.parseToken(token);

            // FIXED: Standardized fallback authority matching the Session Filter onboarding configuration
            String authorityName = (actor.role() != null) 
                    ? "ROLE_" + actor.role().name() 
                    : "ROLE_ONBOARDING";

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authorityName));

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    actor,
                    null,
                    authorities
                );

            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Bind to custom operational thread context
            ActorContext.set(actor);
            log.debug("JWT Actor bound successfully with authority {}: {}", authorityName, actor);

        } catch (Exception e) {
            log.warn("JWT parsing failed: {}", e.getMessage());
            // Clear context immediately if token decoding crashed to avoid cross-contamination
            SecurityContextHolder.clearContext();
            ActorContext.clear();
        }

        try {
            // CRITICAL CHANGE: The rest of the filter chain MUST execute here 
            // while the context variables are still present on the current thread execution path.
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: Wipe thread states out clean after request fully processed
            SecurityContextHolder.clearContext();
            ActorContext.clear();
        }
    }
}