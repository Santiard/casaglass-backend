package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Orden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO optimizado para la TABLA de ordenes en el frontend
 * Contiene solo los campos esenciales para el listado/tabla
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenTablaDTO {
    
    // CAMPOS PRINCIPALES DE LA ORDEN
    private Long id;
    private Long numero;
    private LocalDate fecha;
    private String obra;
    private String descripcion; // Descripción/observaciones adicionales
    private boolean venta;
    private boolean credito;
    private Orden.EstadoOrden estado;
    private boolean facturada;  // Indica si la orden tiene una factura asociada
    private Double subtotal; // Subtotal de la orden (suma de items)
    private Double descuentos; // Descuentos aplicados
    private Double total; // Total final (subtotal - descuentos)
    
    // INFORMACION SIMPLIFICADA DE ENTIDADES RELACIONADAS
    private ClienteTablaDTO cliente;
    private TrabajadorTablaDTO trabajador; 
    private SedeTablaDTO sede;
    
    // INFORMACIÓN DEL CRÉDITO (si es venta a crédito)
    private CreditoTablaDTO creditoDetalle;
    
    // ITEMS COMPLETOS (necesarios para mostrar detalle)
    private List<OrdenItemTablaDTO> items;
    
    /**
     * DTO simplificado para Cliente en tabla de órdenes
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteTablaDTO {
        private String nombre;
    }
    
    /**
     * DTO simplificado para Trabajador en tabla de órdenes
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrabajadorTablaDTO {
        private String nombre;
    }
    
    /**
     * DTO simplificado para Sede en tabla de órdenes
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SedeTablaDTO {
        private String nombre;
    }
    
    /**
     * DTO para OrdenItem en tabla de órdenes
     * Mantiene todos los campos ya que son necesarios para el detalle
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenItemTablaDTO {
        private Long id;
        private ProductoTablaDTO producto;
        private String descripcion;
        private Integer cantidad;
        private Double precioUnitario;
        private Double totalLinea;
    }
    
    /**
     * DTO simplificado para Producto en tabla de órdenes
     * Solo nombre y código como solicitado
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoTablaDTO {
        private String codigo;
        private String nombre;
    }
}
