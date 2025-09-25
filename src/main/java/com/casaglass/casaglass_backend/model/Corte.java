package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;



@Entity
@Table(name = "cortes")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Corte extends Producto {

    @NotNull @Positive
    @Column(name = "largo", nullable = false, precision = 10, scale = 2)
    private BigDecimal largoCm;

    @Column(name="precio", precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;

    @Lob
    private String observacion;
}