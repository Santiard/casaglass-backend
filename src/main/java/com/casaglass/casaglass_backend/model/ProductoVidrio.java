package com.casaglass.casaglass_backend.model;

import java.math.BigDecimal;
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

    @Column(precision = 5, scale = 2)
    private BigDecimal  mm;   // espesor en milímetros

    @Column(precision = 10, scale = 2)
    private BigDecimal  m1m2; // medida o m²

    private Integer laminas;
}
