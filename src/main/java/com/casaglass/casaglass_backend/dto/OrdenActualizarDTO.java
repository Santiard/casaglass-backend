package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para actualizar órdenes desde la tabla
 * Compatible con la estructura de OrdenTablaDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenActualizarDTO {
    
    // CAMPOS PRINCIPALES DE LA ORDEN (ID requerido para actualizacion)
    private Long id;
    private Long numero; // Solo lectura, no se actualiza
    private LocalDate fecha;
    private String obra;
    private String descripcion; // Descripción/observaciones adicionales
    private boolean venta;
    private boolean credito;
    private boolean tieneRetencionFuente = false; // si la orden tiene retención de fuente
    private Double descuentos = 0.0; // Descuentos aplicados a la orden
    
    // IDs DE ENTIDADES RELACIONADAS (para actualizar referencias)
    private Long clienteId;
    private Long trabajadorId; 
    private Long sedeId;
    
    // ITEMS PARA ACTUALIZAR
    private List<OrdenItemActualizarDTO> items;
    
    /**
     * DTO para actualizar OrdenItem
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenItemActualizarDTO {
        private Long id; // null = nuevo item, valor = actualizar existente
        private Long productoId;
        private String descripcion;
        private Integer cantidad;
        private Double precioUnitario;
        private Double totalLinea;
        private boolean eliminar = false; // true = marcar para eliminar
    }
}