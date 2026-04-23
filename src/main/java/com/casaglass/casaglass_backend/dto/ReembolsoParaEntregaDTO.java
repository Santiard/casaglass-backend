package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Reembolso de venta (devolución) listo para incluirse en una entrega de dinero como EGRESO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReembolsoParaEntregaDTO {

    private Long id;
    private LocalDate fecha;
    private Long ordenId;
    private Long numeroOrden;
    private String clienteNombre;
    private String clienteNit;
    /** Monto a devolver al cliente (positivo; en la entrega se contabiliza como egreso). */
    private Double totalReembolso;
    private String motivo;
    private String formaReembolso;
    /** Sede de la venta (orden original), criterio de entrega. */
    private Long sedeId;
    private String sedeNombre;
    /** Siempre "EGRESO" para el armado de la entrega. */
    private String tipoMovimiento;
    private String estado;
}
