package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.EntregaDetalle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDetalleSimpleDTO {
    
    private Long id;
    private Long ordenId;
    private Long abonoId; // ID del abono específico (si aplica)
    private Long numeroOrden;
    private LocalDate fechaOrden;
    private Double montoOrden;
    private Boolean ventaCredito;
    private String clienteNombre;
    private String observaciones;
    private Double abonosDelPeriodo; // Abonos del período para órdenes a crédito (o monto del abono específico)
    
    // Constructor desde entidad (SIN referencia a entrega para evitar ciclos)
    // Sin cálculo de abonos (para compatibilidad)
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle) {
        this.id = detalle.getId();
        this.ordenId = detalle.getOrden() != null ? detalle.getOrden().getId() : null;
        this.abonoId = detalle.getAbono() != null ? detalle.getAbono().getId() : null;
        this.numeroOrden = detalle.getNumeroOrden();
        this.fechaOrden = detalle.getFechaOrden();
        this.montoOrden = detalle.getMontoOrden();
        this.ventaCredito = detalle.getVentaCredito();
        this.clienteNombre = detalle.getClienteNombre();
        this.observaciones = detalle.getObservaciones();
        // Si hay un abono específico, usar su monto; si no, null
        this.abonosDelPeriodo = detalle.getAbono() != null && detalle.getAbono().getTotal() != null 
            ? detalle.getAbono().getTotal() 
            : null;
    }
    
    // Constructor con cálculo de abonos del período
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle, LocalDate fechaDesde, LocalDate fechaHasta, 
                                   com.casaglass.casaglass_backend.service.AbonoService abonoService) {
        this.id = detalle.getId();
        this.ordenId = detalle.getOrden() != null ? detalle.getOrden().getId() : null;
        this.abonoId = detalle.getAbono() != null ? detalle.getAbono().getId() : null;
        this.numeroOrden = detalle.getNumeroOrden();
        this.fechaOrden = detalle.getFechaOrden();
        this.montoOrden = detalle.getMontoOrden();
        this.ventaCredito = detalle.getVentaCredito();
        this.clienteNombre = detalle.getClienteNombre();
        this.observaciones = detalle.getObservaciones();
        
        // Si hay un abono específico, usar su monto directamente
        if (detalle.getAbono() != null && detalle.getAbono().getTotal() != null) {
            this.abonosDelPeriodo = detalle.getAbono().getTotal();
        } else if (detalle.getVentaCredito() != null && detalle.getVentaCredito() && 
            detalle.getOrden() != null && detalle.getOrden().getId() != null &&
            fechaDesde != null && fechaHasta != null && abonoService != null) {
            // Si no hay abono específico, calcular abonos del período (compatibilidad con lógica antigua)
            this.abonosDelPeriodo = abonoService.calcularAbonosOrdenEnPeriodo(
                detalle.getOrden().getId(), fechaDesde, fechaHasta);
        } else {
            this.abonosDelPeriodo = 0.0; // Contado o sin datos
        }
    }
}