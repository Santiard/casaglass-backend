package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de detalle de traslado
 * Incluye información completa del producto incluyendo color
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrasladoDetalleResponseDTO {
    
    private Long id;
    private Integer cantidad;
    private ProductoSimpleDTO producto;
    
    /**
     * Constructor desde entidad TrasladoDetalle
     */
    public TrasladoDetalleResponseDTO(TrasladoDetalle detalle) {
        this.id = detalle.getId();
        this.cantidad = detalle.getCantidad();
        
        // ✅ Mapear producto completo incluyendo color
        if (detalle.getProducto() != null) {
            Producto prod = detalle.getProducto();
            this.producto = new ProductoSimpleDTO(
                prod.getId(),
                prod.getCodigo(),
                prod.getNombre(),
                prod.getColor() != null ? prod.getColor().name() : null  // ✅ INCLUIR COLOR
            );
        }
    }
    
    /**
     * DTO simplificado para Producto con color incluido
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoSimpleDTO {
        private Long id;
        private String codigo;
        private String nombre;
        private String color;  // ✅ Campo color agregado
    }
}
