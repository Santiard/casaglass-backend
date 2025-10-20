package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Orden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenSimpleDTO {
    
    private Long id;
    private Long numero;
    private LocalDate fecha;
    private String obra;
    private boolean venta;
    private boolean credito;
    private Double subtotal;
    private Double total;
    private String estado;
    
    // Constructor desde entidad (SIN referencias a Cliente ni otros objetos complejos)
    public OrdenSimpleDTO(Orden orden) {
        this.id = orden.getId();
        this.numero = orden.getNumero();
        this.fecha = orden.getFecha();
        this.obra = orden.getObra();
        this.venta = orden.isVenta();
        this.credito = orden.isCredito();
        this.subtotal = orden.getSubtotal();
        this.total = orden.getTotal();
        this.estado = orden.getEstado().name();
    }
}