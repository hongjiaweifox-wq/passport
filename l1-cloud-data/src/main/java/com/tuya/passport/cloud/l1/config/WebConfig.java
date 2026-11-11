package com.tuya.passport.cloud.l1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class WebConfig {
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of("http://localhost:5174", "http://127.0.0.1:5174"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Content-Type", "X-Org-Code", "X-User", "X-Role", "If-Match", "If-None-Match", "Idempotency-Key"));
        config.setExposedHeaders(java.util.List.of("ETag", "Cache-Control", "Content-Disposition", "X-Trace-Id"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
