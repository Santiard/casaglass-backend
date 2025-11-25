package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.EntregaDinero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDineroResponseDTO {
    
    private Long id;
    private SedeSimpleDTO sede;
    private TrabajadorSimpleDTO empleado;
    private LocalDate fechaEntrega;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Double monto;
    private Double montoEfectivo;
    private Double montoTransferencia;
    private Double montoCheque;
    private Double montoDeposito;
    private String modalidadEntrega;
    private String estado;
    
    // Detalles como DTOs simples
    private List<EntregaDetalleSimpleDTO> detalles;
    
    // Información adicional calculada
    private Integer totalOrdenes;
    
    // Constructor desde entidad
    public EntregaDineroResponseDTO(EntregaDinero entrega) {
        this.id = entrega.getId();
        this.sede = entrega.getSede() != null ? new SedeSimpleDTO(entrega.getSede()) : null;
        this.empleado = entrega.getEmpleado() != null ? new TrabajadorSimpleDTO(entrega.getEmpleado()) : null;
        this.fechaEntrega = entrega.getFechaEntrega();
        this.fechaDesde = entrega.getFechaDesde();
        this.fechaHasta = entrega.getFechaHasta();
        this.monto = entrega.getMonto();
        this.montoEfectivo = entrega.getMontoEfectivo();
        this.montoTransferencia = entrega.getMontoTransferencia();
        this.montoCheque = entrega.getMontoCheque();
        this.montoDeposito = entrega.getMontoDeposito();
        this.modalidadEntrega = entrega.getModalidadEntrega() != null ? entrega.getModalidadEntrega().name() : null;
        this.estado = entrega.getEstado() != null ? entrega.getEstado().name() : null;
        
        // Convertir detalles a DTOs (sin cálculo de abonos por defecto)
        this.detalles = entrega.getDetalles() != null ? entrega.getDetalles().stream()
                .map(EntregaDetalleSimpleDTO::new)
                .collect(Collectors.toList()) : List.of();
        
        // Calcular información adicional
        this.totalOrdenes = this.detalles.size();
    }
}