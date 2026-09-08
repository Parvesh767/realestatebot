package com.risingbee.realestate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Static resources (HTML, CSS, JS) from classpath
        registry.addResourceHandler("/**")
                .addResourceLocations(
                    "classpath:/static/",
                    "classpath:/public/",
                    "classpath:/resources/",
                    "classpath:/META-INF/resources/"
                );

        // 2. Resolve upload location whether relative (./uploads) or absolute (/var/realestatebot/uploads/)
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        File dir = uploadPath.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        // 3. Map /uploads/** to the resolved file URI
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location, "file:/var/realestatebot/uploads/");
    }
}