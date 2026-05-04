package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformeMensualRangoOrdenesDTO {
    private Long numeroMin;
    private Long numeroMax;
    private Integer cantidad;

    /** Explicación fija para el front (tooltip / impreso). */
    private String criterio;
}
