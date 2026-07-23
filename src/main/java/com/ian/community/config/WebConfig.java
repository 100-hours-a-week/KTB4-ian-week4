package com.ian.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String frontendOrigin;
    private final String storageRoot;

    public WebConfig(
            @Value("${app.frontend-origin}")
            String frontendOrigin,
            @Value("${app.storage.root:./storage/images}")
            String storageRoot
    ) {
        this.frontendOrigin = frontendOrigin;
        this.storageRoot = storageRoot;
    }

    @Override
    public void addCorsMappings(
            CorsRegistry registry
    ) {
        registry
                .addMapping("/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders(
                        "Content-Type",
                        "Accept",
                        "X-XSRF-TOKEN"
                )
                .exposedHeaders(
                        "Location"
                )
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(storageRoot)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
