package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Orden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para la creación de órdenes de venta
 * Incluye la orden creada y los IDs de los cortes creados
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenVentaResponseDTO {
    private Orden orden;                          // La orden creada
    private List<CorteCreacionDTO> cortesCreados; // Lista de cortes creados con sus IDs
}
