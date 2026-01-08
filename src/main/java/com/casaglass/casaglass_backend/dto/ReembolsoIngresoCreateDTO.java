package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para crear un reembolso de ingreso (devolución al proveedor)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReembolsoIngresoCreateDTO {
    
    /**
     * ID del ingreso original que se está reembolsando
     */
    private Long ingresoId;
    
    /**
     * Fecha del retorno (cuándo se devuelve al proveedor)
     * Si no se envía, se usa la fecha actual
     */
    private LocalDate fecha;
    
    /**
     * Número de factura de devolución del proveedor (opcional)
     */
    private String numeroFacturaDevolucion;
    
    /**
     * Motivo o razón del reembolso
     */
    private String motivo;
    
    /**
     * Lista de detalles (productos a devolver)
     */
    private List<ReembolsoIngresoDetalleDTO> detalles;
    
    /**
     * DTO para el detalle de reembolso de ingreso
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReembolsoIngresoDetalleDTO {
        
        /**
         * ID del detalle del ingreso original que se está reembolsando
         * Este es el IngresoDetalle.id del ingreso original
         */
        private Long ingresoDetalleId;
        
        /**
         * Cantidad de productos a devolver
         * Debe ser menor o igual a la cantidad recibida en el ingreso original
         */
        private Double cantidad;
        
        /**
         * Costo unitario al momento del reembolso
         * Si no se envía, se usa el costo del IngresoDetalle original
         */
        private Double costoUnitario;
    }
}

