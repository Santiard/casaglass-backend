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
    private Long reembolsoId; // ID del reembolso (si aplica)
    private Long numeroOrden;
    private LocalDate fechaOrden;
    private Double montoOrden;
    private Boolean ventaCredito;
    private String clienteNombre;
    private String observaciones;
    private Double abonosDelPeriodo; // Abonos del período para órdenes a crédito (o monto del abono específico)
    
    // Método de pago: descripcion para órdenes a contado, metodoPago para abonos
    private String descripcion; // Para órdenes a contado (ventaCredito = false): descripcion de la orden
    private String metodoPago; // Para abonos (ventaCredito = true y abonoId != null): metodoPago del abono
    
    // ✅ TIPO DE MOVIMIENTO: "INGRESO" o "EGRESO"
    private String tipoMovimiento; // INGRESO: órdenes y abonos normales | EGRESO: reembolsos/devoluciones
    
    // Constructor desde entidad (SIN referencia a entrega para evitar ciclos)
    // Sin cálculo de abonos (para compatibilidad)
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle) {
        this.id = detalle.getId();
        this.ordenId = detalle.getOrden() != null ? detalle.getOrden().getId() : null;
        this.abonoId = detalle.getAbono() != null ? detalle.getAbono().getId() : null;
        this.reembolsoId = detalle.getReembolsoVenta() != null ? detalle.getReembolsoVenta().getId() : null;
        this.numeroOrden = detalle.getNumeroOrden();
        this.fechaOrden = detalle.getFechaOrden();
        
        // ✅ MONTO: Usar fuente correcta según el tipo
        if (detalle.getReembolsoVenta() != null) {
            // Es reembolso: usar monto del reembolso (negativo)
            this.montoOrden = -Math.abs(detalle.getReembolsoVenta().getTotalReembolso());
        } else {
            // Es orden/abono normal: usar montoOrden del detalle
            this.montoOrden = detalle.getMontoOrden();
        }
        
        this.ventaCredito = detalle.getVentaCredito();
        this.clienteNombre = detalle.getClienteNombre();
        this.observaciones = detalle.getObservaciones();
        
        // ✅ MAPEAR TIPO DE MOVIMIENTO
        // Si el campo tipoMovimiento está establecido, usarlo
        // Si no, inferir: si tiene reembolsoVenta = EGRESO, de lo contrario = INGRESO
        if (detalle.getTipoMovimiento() != null) {
            this.tipoMovimiento = detalle.getTipoMovimiento().name();
        } else if (detalle.getReembolsoVenta() != null) {
            this.tipoMovimiento = "EGRESO";
        } else {
            this.tipoMovimiento = "INGRESO";
        }
        
        // Si hay un abono específico, usar su monto; si no, null
        this.abonosDelPeriodo = detalle.getAbono() != null && detalle.getAbono().getTotal() != null 
            ? detalle.getAbono().getTotal() 
            : null;
        
        // Método de pago según el tipo:
        // - Para órdenes a contado (ventaCredito = false): descripcion de la orden
        // - Para abonos (ventaCredito = true y abonoId != null): metodoPago del abono
        if (detalle.getVentaCredito() != null && !detalle.getVentaCredito()) {
            // Orden a contado: usar descripcion de la orden
            if (detalle.getOrden() != null) {
                this.descripcion = detalle.getOrden().getDescripcion();
            }
        } else if (detalle.getVentaCredito() != null && detalle.getVentaCredito() && 
                   detalle.getAbono() != null) {
            // Abono: usar metodoPago del abono
            this.metodoPago = detalle.getAbono().getMetodoPago();
        }
    }
    
    // Constructor con cálculo de abonos del período
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle, LocalDate fechaDesde, LocalDate fechaHasta, 
                                   com.casaglass.casaglass_backend.service.AbonoService abonoService) {
        this.id = detalle.getId();
        this.ordenId = detalle.getOrden() != null ? detalle.getOrden().getId() : null;
        this.abonoId = detalle.getAbono() != null ? detalle.getAbono().getId() : null;
        this.reembolsoId = detalle.getReembolsoVenta() != null ? detalle.getReembolsoVenta().getId() : null;
        this.numeroOrden = detalle.getNumeroOrden();
        this.fechaOrden = detalle.getFechaOrden();
        
        // ✅ MONTO: Usar fuente correcta según el tipo
        if (detalle.getReembolsoVenta() != null) {
            // Es reembolso: usar monto del reembolso (negativo)
            this.montoOrden = -Math.abs(detalle.getReembolsoVenta().getTotalReembolso());
        } else {
            // Es orden/abono normal: usar montoOrden del detalle
            this.montoOrden = detalle.getMontoOrden();
        }
        
        this.ventaCredito = detalle.getVentaCredito();
        this.clienteNombre = detalle.getClienteNombre();
        this.observaciones = detalle.getObservaciones();
        
        // ✅ MAPEAR TIPO DE MOVIMIENTO
        // Si el campo tipoMovimiento está establecido, usarlo
        // Si no, inferir: si tiene reembolsoVenta = EGRESO, de lo contrario = INGRESO
        if (detalle.getTipoMovimiento() != null) {
            this.tipoMovimiento = detalle.getTipoMovimiento().name();
        } else if (detalle.getReembolsoVenta() != null) {
            this.tipoMovimiento = "EGRESO";
        } else {
            this.tipoMovimiento = "INGRESO";
        }
        
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
        
        // Método de pago según el tipo:
        // - Para órdenes a contado (ventaCredito = false): descripcion de la orden
        // - Para abonos (ventaCredito = true y abonoId != null): metodoPago del abono
        if (detalle.getVentaCredito() != null && !detalle.getVentaCredito()) {
            // Orden a contado: usar descripcion de la orden
            if (detalle.getOrden() != null) {
                this.descripcion = detalle.getOrden().getDescripcion();
            }
        } else if (detalle.getVentaCredito() != null && detalle.getVentaCredito() && 
                   detalle.getAbono() != null) {
            // Abono: usar metodoPago del abono
            this.metodoPago = detalle.getAbono().getMetodoPago();
        }
    }
}