package com.casaglass.casaglass_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para aplicar múltiples cambios a los detalles de un traslado en una sola transacción.
 * Permite crear, actualizar y eliminar detalles de forma atómica.
 */
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
    }
}

