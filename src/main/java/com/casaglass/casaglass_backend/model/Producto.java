package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    private String posicion;

    // 🔁 Nueva relación con Categoria
    @ManyToOne(fetch = FetchType.EAGER) // EAGER para evitar problemas de carga perezosa
    @JoinColumn(name = "categoria_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Categoria categoria;

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
