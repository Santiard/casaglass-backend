package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Categoria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar productos con información de inventario por sede
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoActualizarDTO {
    
    // Datos básicos del producto
    private Long id;
    private String posicion;
    private String codigo;
    private String nombre;
    private String tipo;
    private String color;
    private Double cantidad; // Cantidad total (información)
    private Double costo;
    private Double precio1;
    private Double precio2;
    private Double precio3;
    private String descripcion;
    private Long version;
    
    // Categoría (solo ID)
    private Categoria categoria;
    
    // Inventario por sede (opcional - si se envía, se actualiza)
    private Integer cantidadInsula;
    private Integer cantidadCentro;
    private Integer cantidadPatios;
}

