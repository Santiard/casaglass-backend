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
    private Double totalVentasDelMes;
    
    /**
     * Total de deudas/créditos activos del mes
     */
    private Double totalDeudasDelMes;
    
    /**
     * Total de abonos registrados del mes
     */
    private Double totalAbonasDelMes;
    
    /**
     * Total entregado del mes (dinero entregado por esta entrega o todas del mes)
     */
    private Double totalEntregadoDelMes;

    /**
     * Total de esta entrega puntual
     */
    private Double totalEstaEntrega;
    
    /**
     * Mes en formato "2026-04"
     */
    private String mes;
    
    /**
     * Nombre de la sede
     */
    private String sede;
    
    /**
     * Nombre del trabajador
     */
    private String trabajador;
    
    /**
     * Nombre del mes en formato "febrero 2026" (campo adicional para referencias)
     */
    private String mesNombre;
}