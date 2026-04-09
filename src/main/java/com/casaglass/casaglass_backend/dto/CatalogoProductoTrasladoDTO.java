package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoProductoTrasladoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private Long categoriaId;
    private String categoriaNombre;
    private String color;
    private Double cantidadSedeOrigen;
    private Double cantidadTotal;
    private Double precio1;
    private Double precio2;
    private Double precio3;
    private boolean esTrasladable;
}
