package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioCorteDTO {
    private Long id;
    private String nombre;
    private Double largoCm;
    private Double precio;
    private Integer cantidadInsula;
    private Integer cantidadCentro;
    private Integer cantidadPatios;
}
