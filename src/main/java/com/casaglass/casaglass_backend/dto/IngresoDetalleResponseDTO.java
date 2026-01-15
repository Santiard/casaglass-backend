package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoDetalleResponseDTO {
    private Long id;
    private ProductoResponseDTO producto;
    private Double cantidad;
    private Double costoUnitario;
    private Double costoUnitarioPonderado;
    private Double totalLinea;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ProductoResponseDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String color;
}
