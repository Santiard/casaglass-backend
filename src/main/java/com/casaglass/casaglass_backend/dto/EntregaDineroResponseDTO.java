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
        // ✅ Envolver en try-catch para manejar excepciones durante la inicialización de colecciones lazy
        try {
            this.id = entrega.getId();
            this.sede = entrega.getSede() != null ? new SedeSimpleDTO(entrega.getSede()) : null;
            this.empleado = entrega.getEmpleado() != null ? new TrabajadorSimpleDTO(entrega.getEmpleado()) : null;
            this.fechaEntrega = entrega.getFechaEntrega();
            this.monto = entrega.getMonto();
            this.montoEfectivo = entrega.getMontoEfectivo();
            this.montoTransferencia = entrega.getMontoTransferencia();
            this.montoCheque = entrega.getMontoCheque();
            this.montoDeposito = entrega.getMontoDeposito();
            this.modalidadEntrega = entrega.getModalidadEntrega() != null ? entrega.getModalidadEntrega().name() : null;
            this.estado = entrega.getEstado() != null ? entrega.getEstado().name() : null;
            
            // ✅ Convertir detalles a DTOs con manejo de errores individual
            // Filtrar cualquier detalle que cause excepción durante su construcción
            if (entrega.getDetalles() != null) {
                this.detalles = entrega.getDetalles().stream()
                        .map(detalle -> {
                            try {
                                return new EntregaDetalleSimpleDTO(detalle);
                            } catch (Exception e) {
                                // Si un detalle causa excepción (referencia huérfana), retornar null
                                // Será filtrado después
                                return null;
                            }
                        })
                        .filter(dto -> dto != null) // Filtrar los DTOs que fallaron
                        .collect(Collectors.toList());
            } else {
                this.detalles = List.of();
            }
            
            // Calcular información adicional
            this.totalOrdenes = this.detalles.size();
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Si ocurre una excepción durante la construcción, inicializar con valores por defecto
            this.id = entrega != null ? entrega.getId() : null;
            this.sede = null;
            this.empleado = null;
            this.fechaEntrega = entrega != null ? entrega.getFechaEntrega() : null;
            this.monto = entrega != null ? entrega.getMonto() : null;
            this.montoEfectivo = entrega != null ? entrega.getMontoEfectivo() : null;
            this.montoTransferencia = entrega != null ? entrega.getMontoTransferencia() : null;
            this.montoCheque = entrega != null ? entrega.getMontoCheque() : null;
            this.montoDeposito = entrega != null ? entrega.getMontoDeposito() : null;
            this.modalidadEntrega = null;
            this.estado = null;
            this.detalles = List.of();
            this.totalOrdenes = 0;
        } catch (Exception e) {
            // Cualquier otra excepción: inicializar con valores por defecto
            this.id = entrega != null ? entrega.getId() : null;
            this.sede = null;
            this.empleado = null;
            this.fechaEntrega = entrega != null ? entrega.getFechaEntrega() : null;
            this.monto = entrega != null ? entrega.getMonto() : null;
            this.montoEfectivo = entrega != null ? entrega.getMontoEfectivo() : null;
            this.montoTransferencia = entrega != null ? entrega.getMontoTransferencia() : null;
            this.montoCheque = entrega != null ? entrega.getMontoCheque() : null;
            this.montoDeposito = entrega != null ? entrega.getMontoDeposito() : null;
            this.modalidadEntrega = null;
            this.estado = null;
            this.detalles = List.of();
            this.totalOrdenes = 0;
        }
    }
}