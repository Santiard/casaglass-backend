package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO optimizado para la TABLA de ingresos en el frontend
 * Contiene solo los campos esenciales para el listado/tabla
 * NO incluye la relación detalles para evitar problemas de LazyInitializationException
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoTablaDTO {
    
    // CAMPOS PRINCIPALES DEL INGRESO
    private Long id;
    private LocalDate fecha;
    private String numeroFactura;
    private String observaciones;
    private Double totalCosto;
    private Boolean procesado;
    
    // SUMA DE TODAS LAS CANTIDADES DE LOS DETALLES
    private Integer cantidadTotal;
    
    // INFORMACIÓN DEL PROVEEDOR (simplificada)
    private ProveedorTablaDTO proveedor;
    
    /**
     * DTO simplificado para Proveedor en tabla de ingresos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProveedorTablaDTO {
        private Long id;
        private String nombre;
        private String nit;
    }
}

