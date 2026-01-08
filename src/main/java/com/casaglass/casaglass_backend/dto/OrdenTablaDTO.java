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
    private boolean tieneRetencionFuente; // Indica si la orden tiene retención de fuente
    private Double retencionFuente; // Valor monetario de la retención en la fuente
    private Orden.EstadoOrden estado;
    private boolean facturada;  // Indica si la orden tiene una factura asociada
    private Double subtotal; // Subtotal de la orden (base imponible SIN IVA)
    private Double iva; // Valor del IVA calculado
    private Double descuentos; // Descuentos aplicados
    private Double total; // Total facturado (subtotal facturado - descuentos, sin restar retención)
    
    // INFORMACION SIMPLIFICADA DE ENTIDADES RELACIONADAS
    private ClienteTablaDTO cliente;
    private TrabajadorTablaDTO trabajador; 
    private SedeTablaDTO sede;
    
    // INFORMACIÓN DEL CRÉDITO (si es venta a crédito)
    private CreditoTablaDTO creditoDetalle;
    
    // ITEMS COMPLETOS (necesarios para mostrar detalle)
    private List<OrdenItemTablaDTO> items;
    
    /**
     * DTO para Cliente en tabla de órdenes
     * Incluye todos los campos del cliente para facturación
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteTablaDTO {
        private Long id;
        private String nit;
        private String nombre;
        private String correo;
        private String ciudad;
        private String direccion;
        private String telefono;
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
        private Double cantidad;
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
