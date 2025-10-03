package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "productos_vidrio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ProductoVidrio extends Producto {

    @Column(nullable = false)
    private Double mm;   // espesor en milímetros

    @Column(nullable = false)
    private Double m1m2; // medida o m²

    private Integer laminas;
}
