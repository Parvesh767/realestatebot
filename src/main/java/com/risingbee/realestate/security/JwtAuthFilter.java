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

import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.enums.*;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


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
            Claims claims = jwtUtil.parseToken(token);

            Long accountId = Long.valueOf(claims.getSubject());

            // 🔑 Build Actor (account-centric)
            Actor actor = new Actor(
                ActorType.BROKER,          // TEMP: expand later
                accountId.toString(),      // externalId not critical here
                accountId
            );

            // ✅ Spring Security authorities (still valid)
            List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + actor.type().name()));

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    accountId,
                    null,
                    authorities
                );

            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // ✅ Set ActorContext (NOT BrokerContext)
            ActorContext.set(actor);

            filterChain.doFilter(request, response);

        } finally {
            ActorContext.clear(); // always safe
        }
    }
}
