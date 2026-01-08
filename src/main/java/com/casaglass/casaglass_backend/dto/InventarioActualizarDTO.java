package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar inventario de un producto en las 3 sedes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventarioActualizarDTO {
    private Double cantidadInsula;
    private Double cantidadCentro;
    private Double cantidadPatios;
}

