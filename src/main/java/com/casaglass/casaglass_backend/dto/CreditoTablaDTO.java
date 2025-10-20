package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.casaglass.casaglass_backend.model.Credito;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoTablaDTO {
    private Long id;
    private LocalDate fechaInicio;
    private Double totalCredito;
    private Double saldoPendiente;
    private Credito.EstadoCredito estado;
    private Double totalAbonado;
}