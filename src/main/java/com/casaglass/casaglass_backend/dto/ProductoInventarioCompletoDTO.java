package com.casaglass.casaglass_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoInventarioCompletoDTO {
    
    // === Datos básicos del producto ===
    private Long id;
    private String codigo;
    private String nombre;
    private String categoria;
    
    // === Datos específicos para vidrios ===
    private Boolean esVidrio;
    private Double mm;           // milímetros (Double para más precisión)
    private Double m1m2;         // metros cuadrados
    private Integer laminas;     // cantidad de láminas
    
    // === Inventario por sede ===
    private Integer cantidadInsula;
    private Integer cantidadCentro;
    private Integer cantidadPatios;
    
    // === Precios ===
    private Double precio1;
    private Double precio2;
    private Double precio3;
    private Double precioEspecial;
    
    // === Campos calculados ===
    private Integer cantidadTotal; // suma de las 3 sedes
    
    // === Constructor con cálculo automático del total ===
    public ProductoInventarioCompletoDTO(Long id, String codigo, String nombre, String categoria,
                                       Boolean esVidrio, Double mm, Double m1m2, Integer laminas,
                                       Integer cantidadInsula, Integer cantidadCentro, Integer cantidadPatios,
                                       Double precio1, Double precio2, Double precio3, Double precioEspecial) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.esVidrio = esVidrio;
        this.mm = mm;
        this.m1m2 = m1m2;
        this.laminas = laminas;
        this.cantidadInsula = cantidadInsula != null ? cantidadInsula : 0;
        this.cantidadCentro = cantidadCentro != null ? cantidadCentro : 0;
        this.cantidadPatios = cantidadPatios != null ? cantidadPatios : 0;
        this.precio1 = precio1;
        this.precio2 = precio2;
        this.precio3 = precio3;
        this.precioEspecial = precioEspecial;
        
        // Calcular total automáticamente
        this.cantidadTotal = this.cantidadInsula + this.cantidadCentro + this.cantidadPatios;
    }
    
    // === Método para recalcular total cuando se modifican cantidades ===
    public void recalcularTotal() {
        this.cantidadTotal = (cantidadInsula != null ? cantidadInsula : 0) + 
                           (cantidadCentro != null ? cantidadCentro : 0) + 
                           (cantidadPatios != null ? cantidadPatios : 0);
    }
}