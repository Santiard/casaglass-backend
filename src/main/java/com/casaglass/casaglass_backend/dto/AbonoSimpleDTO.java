package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Abono;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoSimpleDTO {
    
    private Long id;
    private LocalDate fecha;
    private Double total;
    private String metodoPago;
    private String factura;
    private Double saldo;
    private Long numeroOrden;
    
    // Constructor desde entidad (SIN referencia al crédito completo para evitar ciclos)
    public AbonoSimpleDTO(Abono abono) {
        this.id = abono.getId();
        this.fecha = abono.getFecha();
        this.total = abono.getTotal();
        this.metodoPago = abono.getMetodoPago().name();
        this.factura = abono.getFactura();
        this.saldo = abono.getSaldo();
        this.numeroOrden = abono.getNumeroOrden();
    }
}