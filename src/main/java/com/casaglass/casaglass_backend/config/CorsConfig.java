package com.casaglass.casaglass_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración CORS para permitir solicitudes desde el frontend
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                // Desarrollo local - React/Vite común
                                "http://localhost:3000",
                                "http://localhost:5173",        // Vite default
                                "http://localhost:4200",        // Angular default
                                // Pruebas por IP
                                "http://148.230.87.167:3000",
                                // Producción (ajusta estos dominios cuando los tengas)
                                "https://app.midominio.com",
                                "https://midominio.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}

