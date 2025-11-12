package com.casaglass.casaglass_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración CORS para permitir solicitudes desde el frontend
 * Usa allowedOriginPatterns para compatibilidad con allowCredentials: true
 * 
 * Configuración por entorno:
 * - Desarrollo: localhost y IP del servidor de pruebas
 * - Producción: dominios específicos (configurar en variables de entorno)
 */
@Configuration
public class CorsConfig {

    // Variables de entorno opcionales para URLs de producción
    // Si no se definen, usa los valores por defecto
    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String allowedOriginsEnv;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Si hay URLs definidas en variable de entorno, usarlas
                // Formato: "http://localhost:5173,https://app.midominio.com"
                if (allowedOriginsEnv != null && !allowedOriginsEnv.trim().isEmpty()) {
                    String[] origins = allowedOriginsEnv.split(",");
                    registry.addMapping("/**")
                            .allowedOriginPatterns(origins)
                            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                            .allowedHeaders("*")
                            .allowCredentials(true)
                            .maxAge(3600);
                } else {
                    // Configuración por defecto (desarrollo y producción)
                    registry.addMapping("/**")
                            // Desarrollo local - cualquier puerto en localhost
                            .allowedOriginPatterns(
                                    "http://localhost:*",
                                    // Pruebas por IP - cualquier puerto (servidor de desarrollo/staging)
                                    "http://148.230.87.167:*",
                                    // Producción - REEMPLAZAR con tus dominios reales
                                    // Ejemplo: "https://app.tudominio.com", "https://tudominio.com"
                                    "https://app.midominio.com",
                                    "https://midominio.com"
                            )
                            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                            .allowedHeaders("*")
                            .allowCredentials(true)
                            .maxAge(3600);
                }
            }
        };
    }
}

