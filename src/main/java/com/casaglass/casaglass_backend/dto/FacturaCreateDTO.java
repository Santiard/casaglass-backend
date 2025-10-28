package com.casaglass.casaglass_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para crear una nueva factura
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaCreateDTO {

    /**
     * ID de la orden a facturar
     */
    @NotNull(message = "La orden es obligatoria")
    private Long ordenId;

    /**
     * Fecha de la factura (opcional, por defecto hoy)
     */
    private LocalDate fecha;

    /**
     * Subtotal de la orden
     */
    @NotNull
    @Positive(message = "El subtotal debe ser mayor a 0")
    private Double subtotal;

    /**
     * Descuentos aplicados
     */
    private Double descuentos = 0.0;

    /**
     * IVA aplicado
     */
    private Double iva = 0.0;

    /**
     * Retención en fuente
     */
    private Double retencionFuente = 0.0;

    /**
     * Total de la factura (se calcula automáticamente si no se proporciona)
     */
    private Double total;

    /**
     * Forma de pago
     */
    private String formaPago;

    /**
     * Observaciones
     */
    private String observaciones;

    /**
     * Número de factura personalizado (opcional)
     * Si no se proporciona, se genera automáticamente
     */
    private String numeroFactura;
}

