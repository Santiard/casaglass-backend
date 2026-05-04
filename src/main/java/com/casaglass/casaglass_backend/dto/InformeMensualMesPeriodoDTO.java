package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformeMensualMesPeriodoDTO {
    private Integer year;
    private Integer month;
    private String mesIso;
    private String mesNombre;
}
