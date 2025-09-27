package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;



@Entity
@Table(name = "cortes")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Corte extends Producto {

    @NotNull @Positive
    @Column(name = "largo", nullable = false)
    private Double largoCm;

    @Column(name="precio", nullable = false)
    private Double precio;

    @Lob
    private String observacion;
}