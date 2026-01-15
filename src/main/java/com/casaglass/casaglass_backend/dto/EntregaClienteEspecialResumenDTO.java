package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.EntregaClienteEspecial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaClienteEspecialResumenDTO {

    private Long id;
    private LocalDateTime fechaRegistro;
    private String ejecutadoPor;
    private Integer totalCreditos;
    private Double totalMontoCredito;
    private String observaciones;

    public EntregaClienteEspecialResumenDTO(EntregaClienteEspecial entrega) {
        this.id = entrega.getId();
        this.fechaRegistro = entrega.getFechaRegistro();
        this.ejecutadoPor = entrega.getEjecutadoPor();
        this.totalCreditos = entrega.getTotalCreditos();
        this.totalMontoCredito = entrega.getTotalMontoCredito();
        this.observaciones = entrega.getObservaciones();
    }
}
