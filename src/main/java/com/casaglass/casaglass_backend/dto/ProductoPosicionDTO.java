package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📍 DTO para listar productos con información de posiciones
 * Usado en el endpoint GET /api/productos/posiciones
 * Incluye solo los campos necesarios para mostrar la tabla de posiciones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoPosicionDTO {
    
    private Long id;
    private String codigo;
    private String nombre;
    private String color; // Enum serializado como String (MATE, BLANCO, NEGRO, BRONCE, NA, TRANSPARENTE, etc.)
    private String posicion; // Puede ser null si no tiene posición asignada
    private CategoriaDTO categoria;

    /**
     * DTO simplificado para Categoría
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaDTO {
        private Long id;
        private String nombre;
    }
}

