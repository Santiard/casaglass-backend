package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Factura;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO optimizado para mostrar facturas en tablas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaTablaDTO {

    private Long id;
    private String numero;
    private LocalDate fecha;
    private String obra; // De la orden
    private Double subtotal;
    private Double descuentos;
    private Double iva;
    private Double retencionFuente;
    private Double otrosImpuestos;
    private Double total;
    private String formaPago;
    private EstadoFactura estado;
    private LocalDate fechaPago;
    private String observaciones;

    // Información simplificada de relaciones
    private ClienteTabla cliente;
    private SedeTabla sede;
    private TrabajadorTabla trabajador;
    private OrdenTabla orden;

    // Carmpos anidados
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteTabla {
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SedeTabla {
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrabajadorTabla {
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenTabla {
        private Long numero;
    }

    public enum EstadoFactura {
        PENDIENTE, PAGADA, ANULADA, EN_PROCESO
    }
}

