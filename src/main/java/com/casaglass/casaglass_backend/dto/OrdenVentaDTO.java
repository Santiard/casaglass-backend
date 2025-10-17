package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO optimizado para crear órdenes de venta desde el frontend
 * Contiene toda la información necesaria para realizar una venta real
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenVentaDTO {
    
    // INFORMACIÓN BÁSICA DE LA ORDEN
    private LocalDate fecha; // opcional - si no se envía, se usa la fecha actual
    private String obra; // opcional - descripción del proyecto/obra
    private boolean venta = true; // por defecto true para ventas
    private boolean credito = false; // si es venta a crédito
    private boolean incluidaEntrega = false; // si incluye entrega
    
    // IDs DE ENTIDADES RELACIONADAS (requeridos)
    private Long clienteId; // OBLIGATORIO
    private Long sedeId; // OBLIGATORIO - sede donde se realiza la venta
    private Long trabajadorId; // OPCIONAL - vendedor encargado
    
    // ITEMS DE LA VENTA (mínimo 1 item requerido)
    private List<OrdenItemVentaDTO> items;
    
    /**
     * DTO para items de venta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenItemVentaDTO {
        private Long productoId; // OBLIGATORIO - producto a vender
        private String descripcion; // OPCIONAL - descripción personalizada
        private Integer cantidad; // OBLIGATORIO - cantidad a vender (min: 1)
        private Double precioUnitario; // OBLIGATORIO - precio unitario
        
        // totalLinea se calcula automáticamente en el backend
    }
}