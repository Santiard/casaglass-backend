package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Credito;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoResponseDTO {
    
    private Long id;
    private ClienteSimpleDTO cliente;
    private OrdenSimpleDTO orden;
    private LocalDate fechaInicio;
    private LocalDate fechaCierre;
    private Double totalCredito;
    private Double totalAbonado;
    private Double saldoPendiente;
    private String estado;
    private String observaciones;
    private List<AbonoSimpleDTO> abonos;
    
    // Constructor desde entidad
    public CreditoResponseDTO(Credito credito) {
        this.id = credito.getId();
        this.cliente = new ClienteSimpleDTO(credito.getCliente());
        this.orden = new OrdenSimpleDTO(credito.getOrden());
        this.fechaInicio = credito.getFechaInicio();
        this.fechaCierre = credito.getFechaCierre();
        this.totalCredito = credito.getTotalCredito();
        this.totalAbonado = credito.getTotalAbonado();
        this.saldoPendiente = credito.getSaldoPendiente();
        this.estado = credito.getEstado().name();
        this.observaciones = credito.getObservaciones();
        
        // Convertir abonos a DTOs simples (SIN referencias circulares)
        this.abonos = credito.getAbonos().stream()
                .map(AbonoSimpleDTO::new)
                .collect(Collectors.toList());
    }
}