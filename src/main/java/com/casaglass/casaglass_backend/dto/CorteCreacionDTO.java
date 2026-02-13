package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un corte creado durante la creación de una orden
 * Se usa para devolver al frontend los IDs de los cortes creados
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorteCreacionDTO {
    private Long corteId;              // ID del nuevo producto corte creado
    private Integer medidaSolicitada;  // Medida en cm del corte solicitado
    private Long productoBase;         // ID del producto original del que se cortó
}
