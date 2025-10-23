package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorteInventarioCompletoDTO {
    
    // Datos básicos del Corte (hereda de Producto)
    private Long id;
    private String codigo;
    private String nombre;
    private String categoria;
    private String tipo; // Campo tipo
    private String color; // Campo color
    
    // Datos específicos de Corte
    private Double largoCm;
    private Double precio;
    private String observacion;
    
    // Inventario por sede
    private Integer cantidadInsula;
    private Integer cantidadCentro;
    private Integer cantidadPatios;
    private Integer cantidadTotal;
    
    // Precios heredados de Producto
    private Double precio1;
    private Double precio2;
    private Double precio3;
    
    // Constructor principal con cálculo automático de total
    public CorteInventarioCompletoDTO(Long id, String codigo, String nombre, String categoria, String tipo, String color,
                                     Double largoCm, Double precio, String observacion,
                                     Integer cantidadInsula, Integer cantidadCentro, Integer cantidadPatios,
                                     Double precio1, Double precio2, Double precio3) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.tipo = tipo;
        this.color = color;
        this.largoCm = largoCm;
        this.precio = precio;
        this.observacion = observacion;
        this.cantidadInsula = cantidadInsula != null ? cantidadInsula : 0;
        this.cantidadCentro = cantidadCentro != null ? cantidadCentro : 0;
        this.cantidadPatios = cantidadPatios != null ? cantidadPatios : 0;
        this.precio1 = precio1;
        this.precio2 = precio2;
        this.precio3 = precio3;
        
        // Calcular total automáticamente
        recalcularTotal();
    }
    
    // Método para recalcular el total cuando se modifiquen las cantidades
    public void recalcularTotal() {
        this.cantidadTotal = (cantidadInsula != null ? cantidadInsula : 0) + 
                            (cantidadCentro != null ? cantidadCentro : 0) + 
                            (cantidadPatios != null ? cantidadPatios : 0);
    }
    
    // Setters que recalculan automáticamente
    public void setCantidadInsula(Integer cantidadInsula) {
        this.cantidadInsula = cantidadInsula != null ? cantidadInsula : 0;
        recalcularTotal();
    }
    
    public void setCantidadCentro(Integer cantidadCentro) {
        this.cantidadCentro = cantidadCentro != null ? cantidadCentro : 0;
        recalcularTotal();
    }
    
    public void setCantidadPatios(Integer cantidadPatios) {
        this.cantidadPatios = cantidadPatios != null ? cantidadPatios : 0;
        recalcularTotal();
    }
}