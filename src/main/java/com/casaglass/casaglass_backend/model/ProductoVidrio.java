package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos_vidrio")
@PrimaryKeyJoinColumn(name = "id")  // ✅ IMPORTANTE: Usa el mismo ID de la tabla productos
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductoVidrio extends Producto {

    @Column(nullable = false)
    private Double mm;   // espesor en milímetros

    @Column(nullable = false)
    private Double m1;  // medida 1

    @Column(nullable = false)
    private Double m2;  // medida 2

    @Column(nullable = false)
    private Double m1m2;  // m1 * m2 (calculado automáticamente)
    
    /**
     * Calcula m1m2 automáticamente antes de persistir o actualizar
     */
    @PrePersist
    @PreUpdate
    private void calcularM1M2() {
        if (m1 != null && m2 != null) {
            this.m1m2 = m1 * m2;
        } else if (m1m2 == null) {
            // Si m1 o m2 son null, establecer 0 como valor por defecto
            this.m1m2 = 0.0;
        }
    }
}
