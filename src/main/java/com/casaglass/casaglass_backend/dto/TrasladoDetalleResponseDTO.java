package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private Double cantidad;
    private ProductoSimpleDTO producto;

    @Schema(description = "Id plano del producto entero descontado en Insula (duplicado para edición rápida)")
    private Long productoInventarioADescontarSede1Id;
    @Schema(description = "Mismo criterio que producto: codigo, nombre, color")
    private ProductoSimpleDTO productoInventarioADescontarSede1;
    
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
        if (detalle.getProductoInventarioADescontarSede1() != null) {
            Producto pd = detalle.getProductoInventarioADescontarSede1();
            this.productoInventarioADescontarSede1Id = pd.getId();
            this.productoInventarioADescontarSede1 = new ProductoSimpleDTO(
                    pd.getId(),
                    pd.getCodigo(),
                    pd.getNombre(),
                    pd.getColor() != null ? pd.getColor().name() : null);
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
