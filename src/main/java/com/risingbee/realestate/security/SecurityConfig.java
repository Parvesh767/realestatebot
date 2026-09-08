package com.risingbee.realestate.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.risingbee.realestate.security.filter.ActorSessionFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ActorSessionFilter actorSessionFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(List.of(
                        "http://localhost:[*]",
                        "http://127.0.0.1:[*]",
                        "https://*.ngrok-free.dev",
                        "https://*.ngrok.io",
                        "https://*.razorpay.com"
                    ));
                
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                return config;
            }))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // 1. Public Pages, Auth, Error, & Static Assets
                .requestMatchers(
                    "/",
                    "/error",               // 👈 Prevents 403 when Spring handles exceptions
                    "/search/**",
                    "/properties/**",
                    "/web/auth/**",
                    "/api/places/**",
                    "/api/whatsapp/webhook/**",
                    "/api/payments/webhook/**",
                    "/api/payment/success",
                    "/uploads/**",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()

                // 2. Protected Views & APIs (Requires BROKER, OWNER, or ADMIN)
                .requestMatchers("/tenant/**").hasAnyRole("TENANT", "USER", "BROKER", "ADMIN")
                .requestMatchers("/dashboard.html", "/admin/**").hasAnyRole("BROKER", "OWNER", "ADMIN")
                .requestMatchers("/api/leads/**", "/api/broker/**", "/api/accounts/**").hasAnyRole("BROKER", "ADMIN")

                // 3. Fallback Catch-All
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String acceptHeader = request.getHeader("Accept");
                    String requestUri = request.getRequestURI();

                    if (requestUri.startsWith("/api/") || (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE))) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\": \"UNAUTHORIZED\", \"message\": \"Authentication required\"}");
                    } else {
                        response.sendRedirect("/web/auth/login");
                    }
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(actorSessionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}