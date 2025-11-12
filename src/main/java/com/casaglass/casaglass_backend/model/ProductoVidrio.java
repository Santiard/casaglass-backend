package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos_vidrio")
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
}
