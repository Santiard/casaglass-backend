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
    private Long numeroOrden;
    private LocalDate fechaOrden;
    private Double montoOrden;
    private Boolean ventaCredito;
    private String clienteNombre;
    private String observaciones;
    
    // Constructor desde entidad (SIN referencia a entrega para evitar ciclos)
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle) {
        this.id = detalle.getId();
        this.ordenId = detalle.getOrden() != null ? detalle.getOrden().getId() : null;
        this.numeroOrden = detalle.getNumeroOrden();
        this.fechaOrden = detalle.getFechaOrden();
        this.montoOrden = detalle.getMontoOrden();
        this.ventaCredito = detalle.getVentaCredito();
        this.clienteNombre = detalle.getClienteNombre();
        this.observaciones = detalle.getObservaciones();
    }
}