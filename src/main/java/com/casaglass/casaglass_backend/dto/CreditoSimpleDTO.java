package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Credito;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoSimpleDTO {
    
    private Long id;
    private LocalDate fechaInicio;
    private LocalDate fechaCierre;
    private Double totalCredito;
    private Double totalAbonado;
    private Double saldoPendiente;
    private String estado;
    private String observaciones;
    
    // Constructor desde entidad (SIN referencias circulares a orden ni abonos)
    public CreditoSimpleDTO(Credito credito) {
        this.id = credito.getId();
        this.fechaInicio = credito.getFechaInicio();
        this.fechaCierre = credito.getFechaCierre();
        this.totalCredito = credito.getTotalCredito();
        this.totalAbonado = credito.getTotalAbonado();
        this.saldoPendiente = credito.getSaldoPendiente();
        this.estado = credito.getEstado().name();
        this.observaciones = credito.getObservaciones();
        
        // NO incluimos abonos aquí para evitar ciclos
        // NO incluimos orden aquí para evitar ciclos
    }
}