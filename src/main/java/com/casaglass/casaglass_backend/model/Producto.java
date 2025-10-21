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

    // 🔧 COMENTAR temporalmente para debug
    // @Version
    private Long version;

    private String posicion;

    // 🔁 Nueva relación con Categoria
    @ManyToOne(fetch = FetchType.EAGER) // EAGER para evitar problemas de carga perezosa
    @JoinColumn(name = "categoria_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Categoria categoria;

    // 🆕 Nuevo campo tipo como enum
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoProducto tipo;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String nombre;
    
    // 🆕 Campo color como enum
    @Enumerated(EnumType.STRING)
    @Column(name = "color")
    private ColorProducto color;
    
    private Integer cantidad;
    private Double costo;
    private Double precio1;
    private Double precio2;
    private Double precio3;
    private Double precioEspecial;

    @Lob
    private String descripcion;
}
