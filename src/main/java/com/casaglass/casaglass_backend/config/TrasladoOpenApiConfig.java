package com.casaglass.casaglass_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Título/versión de la app en Swagger. Los esquemas concretos de traslado están en
 * {@code @Schema} de {@code TrasladoDetalle}, {@code TrasladoDetalleBatchDTO} y anotaciones en controladores.
 */
@Configuration
public class TrasladoOpenApiConfig {

    @Bean
    public OpenAPI casaglassOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Casaglass API (fragmento traslados)")
                        .version("1")
                        .description("Listado completo de endpoints: ver tags Traslados y subsecciones en Swagger UI."));
    }
}
