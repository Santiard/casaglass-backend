package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Inheritance(strategy = InheritanceType.JOINED)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    private String posicion;

    private String categoria; // Vidrio, Aluminio, Accesorio

    @Column(unique = true, nullable = false)
    private String codigo;

    private String nombre;

    private String color;

    private Integer cantidad;

    private Double costo;

    private Double precio1;

    private Double precio2;

    private Double precio3;

    private Double precioEspecial;

    @Lob
    private String descripcion;
}
 ;
