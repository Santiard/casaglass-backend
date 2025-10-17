package com.casaglass.casaglass_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion global para permitir que Jackson
 * deserialice LocalDate, LocalDateTime, etc. desde cadenas ISO (YYYY-MM-DD)
 */
@Configuration
public class JacksonConfig {

    private final ObjectMapper objectMapper;

    public JacksonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void setup() {
        objectMapper.registerModule(new JavaTimeModule());
    }
}