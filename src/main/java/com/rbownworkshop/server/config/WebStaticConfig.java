package com.rbownworkshop.server.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Expose các file đã upload (ảnh) qua URL /uploads/images/**. */
@Configuration
public class WebStaticConfig implements WebMvcConfigurer {

    @Value("${app.upload.image-dir}")
    private String imageDir;

    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
        Path abs = Paths.get(imageDir).toAbsolutePath().normalize();
        String location = abs.toUri().toString();
        if (!location.endsWith("/")) location += "/";

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(location)
                .setCachePeriod(60 * 60 * 24 * 7);
    }
}