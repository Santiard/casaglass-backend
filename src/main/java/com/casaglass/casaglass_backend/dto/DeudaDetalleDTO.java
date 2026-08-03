package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeudaDetalleDTO {
    private Long creditoId;
    private Long ordenId;
    private String cliente;
    private LocalDate fechaInicio;
    private Double totalCredito;
    private Double saldoPendiente;
}
