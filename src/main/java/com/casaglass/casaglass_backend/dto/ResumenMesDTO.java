package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMesDTO {
    /**
     * TOTAL DE DINERO ENTREGADO
     * Dinero entregado en esta entrega puntual.
     */
    private Double totalDineroEntregado;

    /**
     * TOTAL VENTAS DEL MES (sede del contexto cuando aplica): suma de {@code orden.total} con venta en el mes calendario.
     * No es la suma de montos de entregas del mes ({@link #totalDineroEntregado} es solo la entrega actual).
     */
    private Double totalDelMes;

    /**
     * TOTAL DEUDAS MENSUALES
     * Suma de saldos pendientes de créditos activos del mes para la sede de la entrega.
     */
    private Double totalDeudasMensuales;
    
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

    /**
     * TOTAL CRÉDITOS ACTIVOS HISTÓRICO
     * Suma del saldo pendiente de TODOS los créditos ABIERTOS de la sede,
     * sin importar en qué mes fueron creados.
     */
    private Double totalCreditosActivosHistorico;
}