package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para mostrar abonos disponibles para incluir en una entrega
 * Solo muestra abonos de órdenes a crédito que aún no han sido entregados
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoParaEntregaDTO {
    
    private Long id; // ID del abono
    private Long ordenId; // ID de la orden a la que pertenece
    private Long numeroOrden; // Número de la orden
    private LocalDate fechaOrden; // Fecha de la orden
    private LocalDate fechaAbono; // Fecha del abono
    private String clienteNombre; // Nombre del cliente
    private String clienteNit; // NIT del cliente
    private Double montoAbono; // Monto del abono
    private Double montoOrden; // Monto total de la orden
    private String metodoPago; // Método de pago del abono
    private String factura; // Número de factura/recibo
    private String obra; // Obra de la orden
    private String sedeNombre; // Nombre de la sede
    private String trabajadorNombre; // Nombre del trabajador
    
    // 💰 MONTOS POR MÉTODO DE PAGO
    private Double montoEfectivo;
    private Double montoTransferencia;
    private Double montoCheque;
    private Double montoRetencion; // Retención en la fuente de este abono
    
    private boolean yaEntregado; // Si el abono ya está en otra entrega (basado en si la orden está incluida)
    private String estadoOrden; // ACTIVA, ANULADA
    private boolean ventaOrden; // true si la orden asociada es una venta (no compra)
}

