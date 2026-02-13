package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMesDTO {
    
    /**
     * Total de ventas de todas las órdenes del mes
     */
    private Double totalVentasMes;
    
    /**
     * Total de saldos pendientes de créditos activos del mes
     */
    private Double totalCreditosActivosMes;
    
    /**
     * Nombre del mes en formato "febrero 2026"
     */
    private String mesNombre;
}