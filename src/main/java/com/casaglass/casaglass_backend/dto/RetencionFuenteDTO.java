package com.casaglass.casaglass_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 💰 DTO PARA ACTUALIZAR RETENCIÓN DE FUENTE EN UNA ORDEN
 * 
 * Este DTO se usa exclusivamente para actualizar los campos de retención de fuente
 * sin necesidad de enviar todos los datos de la orden (items, cliente, sede, etc.)
 * 
 * Endpoint: PUT /api/ordenes/{id}/retencion-fuente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetencionFuenteDTO {
    
    /**
     * Indica si la orden tiene retención de fuente aplicada
     * OBLIGATORIO
     */
    @NotNull(message = "El campo tieneRetencionFuente es obligatorio")
    private Boolean tieneRetencionFuente;
    
    /**
     * Valor monetario de la retención en la fuente
     * Si tieneRetencionFuente = false, este valor debe ser 0.0
     */
    @NotNull(message = "El valor de retención es obligatorio")
    private Double retencionFuente;
    
    /**
     * Valor del IVA recalculado (opcional)
     * Si no se envía, el backend lo calcula automáticamente
     */
    private Double iva;
}
