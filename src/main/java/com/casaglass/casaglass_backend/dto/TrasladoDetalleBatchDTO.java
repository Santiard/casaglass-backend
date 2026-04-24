package com.casaglass.casaglass_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para aplicar múltiples cambios a los detalles de un traslado en una sola transacción.
 * Permite crear, actualizar y eliminar detalles de forma atómica.
 * <p><b>Nombres respecto al POST/PUT con entidad {@code TrasladoDetalle}:</b> en {@code crear[]} se usa
 * <code>productoInventarioADescontarSede1Id</code> (Long plano). En cuerpos que envían la entidad
 * completa, el mismo dato va como <code>productoInventarioADescontarSede1: { \"id\" }</code>.</p>
 * <p>Reintentos: si un batch falla a mitad, consulte {@code GET /api/traslados/{id}/detalles} o
 * {@code GET /api/traslados/{id}} antes de reenviar creaciones, para no duplicar líneas.</p>
 */
@Schema(name = "TrasladoDetalleBatchDTO", description = "Cambios atómicos; campo crear.productoInventarioADescontarSede1Id vs entidad anidada productoInventarioADescontarSede1.id")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrasladoDetalleBatchDTO {
    
    /**
     * Detalles nuevos a crear (sin ID)
     */
    @Valid
    private List<DetalleCrearDTO> crear;
    
    /**
     * Detalles existentes a actualizar (con ID)
     */
    @Valid
    private List<DetalleActualizarDTO> actualizar;
    
    /**
     * IDs de detalles a eliminar
     */
    private List<Long> eliminar;
    
    /**
     * DTO para crear un nuevo detalle
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleCrearDTO {
        @NotNull(message = "El producto es obligatorio")
        private Long productoId;
        
        @NotNull(message = "La cantidad es obligatoria")
        private Double cantidad;

        @Schema(description = "1→2/3, línea corte: id del producto entero a descontar en Insula. Omitir si no aplica descuento de entero.")
        private Long productoInventarioADescontarSede1Id;
    }
    
    /**
     * DTO para actualizar un detalle existente
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleActualizarDTO {
        @NotNull(message = "El ID del detalle es obligatorio")
        private Long detalleId;
        
        private Long productoId; // Opcional: si se envía, cambia el producto
        
        private Double cantidad; // Opcional: si se envía, cambia la cantidad

        @Schema(description = "Si true, quita el descuento de entero en Insula (productoInventarioADescontarSede1).")
        private Boolean limpiarProductoInventarioADescontarSede1;

        @Schema(description = "Actualiza el producto a descontar en Insula; ignorado si se usa limpiar en true")
        private Long productoInventarioADescontarSede1Id;
    }
}

