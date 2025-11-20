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
    private Double montoEsperado;
    private Double montoGastos;
    private Double montoEntregado;
    private Double montoEfectivo;
    private Double montoTransferencia;
    private Double montoCheque;
    private Double montoDeposito;
    private Double diferencia;
    private String modalidadEntrega;
    private String estado;
    private String observaciones;
    private String numeroComprobante;
    
    // Detalles y gastos como DTOs simples
    private List<EntregaDetalleSimpleDTO> detalles;
    private List<GastoSedeSimpleDTO> gastos;
    
    // Información adicional calculada
    private Integer totalOrdenes;
    private Integer totalGastos;
    private Double porcentajeGastos; // gastos/esperado * 100
    
    // Constructor desde entidad
    public EntregaDineroResponseDTO(EntregaDinero entrega) {
        this.id = entrega.getId();
        this.sede = entrega.getSede() != null ? new SedeSimpleDTO(entrega.getSede()) : null;
        this.empleado = entrega.getEmpleado() != null ? new TrabajadorSimpleDTO(entrega.getEmpleado()) : null;
        this.fechaEntrega = entrega.getFechaEntrega();
        this.fechaDesde = entrega.getFechaDesde();
        this.fechaHasta = entrega.getFechaHasta();
        this.montoEsperado = entrega.getMontoEsperado();
        this.montoGastos = entrega.getMontoGastos();
        this.montoEntregado = entrega.getMontoEntregado();
        this.montoEfectivo = entrega.getMontoEfectivo();
        this.montoTransferencia = entrega.getMontoTransferencia();
        this.montoCheque = entrega.getMontoCheque();
        this.montoDeposito = entrega.getMontoDeposito();
        this.diferencia = entrega.getDiferencia();
        this.modalidadEntrega = entrega.getModalidadEntrega() != null ? entrega.getModalidadEntrega().name() : null;
        this.estado = entrega.getEstado() != null ? entrega.getEstado().name() : null;
        this.observaciones = entrega.getObservaciones();
        this.numeroComprobante = entrega.getNumeroComprobante();
        
        // Convertir detalles y gastos a DTOs (sin cálculo de abonos por defecto)
        this.detalles = entrega.getDetalles() != null ? entrega.getDetalles().stream()
                .map(EntregaDetalleSimpleDTO::new)
                .collect(Collectors.toList()) : List.of();
        
        this.gastos = entrega.getGastos() != null ? entrega.getGastos().stream()
                .map(GastoSedeSimpleDTO::new)
                .collect(Collectors.toList()) : List.of();
        
        // Calcular información adicional
        this.totalOrdenes = this.detalles.size();
        this.totalGastos = this.gastos.size();
        this.porcentajeGastos = this.montoEsperado != null && this.montoEsperado > 0 ? 
            (this.montoGastos != null ? this.montoGastos : 0.0) / this.montoEsperado * 100 : 0.0;
    }
}