package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas
 * Compatible con el formato esperado por el frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;           // Array con los registros de la página actual
    private long totalElements;        // Total de registros que cumplen los filtros
    private int totalPages;            // Total de páginas
    private int page;                  // Página actual (1-indexed)
    private int size;                  // Tamaño de página
    private boolean hasNext;           // Si hay página siguiente
    private boolean hasPrevious;       // Si hay página anterior

    /**
     * Constructor helper para crear respuesta paginada
     */
    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
            content,
            totalElements,
            totalPages,
            page,
            size,
            page < totalPages,
            page > 1
        );
    }
}

