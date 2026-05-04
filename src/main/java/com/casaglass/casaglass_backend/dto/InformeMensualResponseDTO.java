package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformeMensualResponseDTO {

    /** PREVIEW | CERRADO */
    private String origen;

    private SedeSimpleDTO sede;
    private InformeMensualMesPeriodoDTO periodo;

    private Double ventasMes;
    private Double dineroRecogidoMes;
    private Double deudasMes;
    private Double deudasActivasTotales;
    private Double valorInventario;

    private InformeMensualRangoOrdenesDTO ordenesVentasMes;
}
