package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO específico para órdenes a crédito
 * Usado en GET /api/ordenes/credito?clienteId=X
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCreditoDTO {
    
    private Long id;
    private Long numero;
    private LocalDate fecha;
    private Double total;
    private boolean credito;
    private String numeroFactura; // Número de la factura asociada ("-" si no tiene)
    
    private CreditoDetalleDTO creditoDetalle;
    
    /**
     * DTO simplificado para información del crédito
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditoDetalleDTO {
        private Long creditoId;
        private Double saldoPendiente;
    }
}

