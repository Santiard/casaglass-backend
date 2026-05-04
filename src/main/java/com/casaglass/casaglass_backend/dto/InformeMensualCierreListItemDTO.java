package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformeMensualCierreListItemDTO {
    private Long id;
    private Integer year;
    private Integer month;
    private String mesIso;
    private Double ventasMes;
    private Double dineroRecogidoMes;
}
