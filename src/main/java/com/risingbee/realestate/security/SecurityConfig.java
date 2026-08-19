package com.risingbee.realestate.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.risingbee.realestate.security.filter.ActorSessionFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
            config.setAllowedOriginPatterns(List.of("http://localhost:[*]", "https://*.yourdomain.com", "https://*.ngrok-free.dev", "https://*.ngrok-free.app")); 
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            return config;
        }));

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
            		
            		// 1. Allow Static HTML, CSS, JS, and Images
                    .requestMatchers(
                        "/dashboard.html",
                        "/",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico"
                    ).permitAll()
            		
                // Public registration, login, webhooks & payment routes
                .requestMatchers(
                    "/api/auth/**",    
                    "/api/whatsapp/**",
                    "/api/accounts/**",
                    "/api/leads/**",
                    "/search", 
                    "/search/results",
                    "/auth/**",
                    "/web/auth/**",
                    "/payment/**"
                ).permitAll()
                .requestMatchers("/uploads/**").permitAll()
                
                // Onboarding Access
                .requestMatchers("/auth/onboarding/**", "/api/profile/complete").hasAnyRole("ONBOARDING", "BROKER", "OWNER", "TENANT")
                
                // Target Role Protected Access routes
                .requestMatchers("/api/broker/**").hasRole("BROKER")
                .requestMatchers("/admin/**").hasAnyRole("BROKER", "OWNER", "ADMIN")
                
                // Fallback catch-all authentication
                .anyRequest().authenticated()
            )
            // 2. Add JWT Filter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            // 3. Add Session Filter
            .addFilterBefore(
                actorSessionFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        log.debug("Security Filter Chain configured successfully.");

        return http.build();
    }
}