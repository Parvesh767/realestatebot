package com.risingbee.realestate.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

//    private final BrokerResolutionInterceptor brokerInterceptor;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        // exclude static resources (optional pattern)
//        registry.addInterceptor(brokerInterceptor).addPathPatterns("/**");
//    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/uploads/**")
                .allowedOrigins("*")
                .allowedMethods("GET");
    }
}

