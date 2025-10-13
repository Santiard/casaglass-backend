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
    private Double m1m2; // medida o m²

    private Integer laminas;
}
