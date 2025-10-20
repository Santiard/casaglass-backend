package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Orden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenResponseDTO {
    
    private Long id;
    private Long numero;
    private LocalDate fecha;
    private ClienteSimpleDTO cliente;
    private SedeSimpleDTO sede;
    private TrabajadorSimpleDTO trabajador;
    private String obra;
    private boolean venta;
    private boolean credito;
    private Double subtotal;
    private Double total;
    private boolean incluidaEntrega;
    private String estado;
    
    // Solo incluimos el crédito si existe (SIN referencias circulares)
    private CreditoSimpleDTO creditoDetalle;
    
    // Constructor desde entidad
    public OrdenResponseDTO(Orden orden) {
        this.id = orden.getId();
        this.numero = orden.getNumero();
        this.fecha = orden.getFecha();
        this.cliente = new ClienteSimpleDTO(orden.getCliente());
        this.sede = new SedeSimpleDTO(orden.getSede());
        this.trabajador = orden.getTrabajador() != null ? new TrabajadorSimpleDTO(orden.getTrabajador()) : null;
        this.obra = orden.getObra();
        this.venta = orden.isVenta();
        this.credito = orden.isCredito();
        this.subtotal = orden.getSubtotal();
        this.total = orden.getTotal();
        this.incluidaEntrega = orden.isIncluidaEntrega();
        this.estado = orden.getEstado().name();
        
        // Solo incluir crédito si existe (SIN la referencia de vuelta a orden)
        this.creditoDetalle = orden.getCreditoDetalle() != null ? 
            new CreditoSimpleDTO(orden.getCreditoDetalle()) : null;
    }
}