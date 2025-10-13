package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioProductoDTO {
    private Long id;
    private String nombre;
    private Integer cantidadInsula;
    private Integer cantidadCentro;
    private Integer cantidadPatios;
}
