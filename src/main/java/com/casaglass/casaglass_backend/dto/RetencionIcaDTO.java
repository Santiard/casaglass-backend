package com.casaglass.casaglass_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 💰 DTO PARA ACTUALIZAR RETENCIÓN ICA EN UNA ORDEN
 * 
 * Este DTO se usa exclusivamente para actualizar los campos de retención ICA
 * sin necesidad de enviar todos los datos de la orden (items, cliente, sede, etc.)
 * 
 * Endpoint: PUT /api/ordenes/{id}/retencion-ica
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetencionIcaDTO {
    
    /**
     * Indica si la orden tiene retención ICA aplicada
     * OBLIGATORIO
     */
    @NotNull(message = "El campo tieneRetencionIca es obligatorio")
    private Boolean tieneRetencionIca;
    
    /**
     * Porcentaje de retención ICA (0-100)
     * Si no se especifica, se usa el valor por defecto de BusinessSettings
     */
    private Double porcentajeIca;
    
    /**
     * Valor monetario de la retención ICA
     * Si tieneRetencionIca = false, este valor debe ser 0.0
     */
    @NotNull(message = "El valor de retención ICA es obligatorio")
    private Double retencionIca;
    
    /**
     * Valor del IVA recalculado (opcional)
     * Si no se envía, el backend lo calcula automáticamente
     */
    private Double iva;
}

