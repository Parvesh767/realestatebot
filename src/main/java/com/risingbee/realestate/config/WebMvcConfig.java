package com.risingbee.realestate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Static resources (HTML, CSS, JS) from src/main/resources/static/
        registry.addResourceHandler("/**")
                .addResourceLocations(
                    "classpath:/static/",
                    "classpath:/public/",
                    "classpath:/resources/",
                    "classpath:/META-INF/resources/"
                );

        // 2. Uploaded photos / files (mapped to /uploads/** instead of root)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/var/realestatebot/uploads/");
    }
}