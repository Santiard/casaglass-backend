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
    private String numeroFactura;
    private LocalDate fecha;
    private String obra; // De la orden
    private Double subtotal;
    private Double descuentos;
    private Double iva;
    private Double retencionFuente;
    private Double total;
    private String formaPago;
    private EstadoFactura estado;
    private LocalDate fechaPago;
    private String observaciones;

    // Información simplificada de relaciones
    private ClienteTabla cliente;
    private OrdenTabla orden;

    // Campos anidados
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteTabla {
        private String nombre;
        private String nit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenTabla {
        private Long id;      // ID interno de la orden (para navegar a detalles)
        private Long numero;  // Número legible de la orden
    }

    public enum EstadoFactura {
        PENDIENTE, PAGADA, ANULADA, EN_PROCESO
    }
}

