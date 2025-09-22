package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @Column(nullable = false)
    private Integer cantidad;
}