package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Categoria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorteActualizarCompletoDTO {
    private Long id;
    private String posicion;
    private String codigo;
    private String nombre;
    private Categoria categoria;
    private String tipo;
    private String color;
    private Double largoCm;
    private Double cantidad;
    private Double costo;
    private Double precio1;
    private Double precio2;
    private Double precio3;
    private String descripcion;

    private Double cantidadInsula;
    private Double cantidadCentro;
    private Double cantidadPatios;
}