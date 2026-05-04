package com.casaglass.casaglass_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InformeMensualCierreRequestDTO {

    @NotNull
    private Long sedeId;

    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    /** Si es false no se ejecuta el cierre (400). Por defecto se asume true en servicio si null. */
    private Boolean confirmar;
}
