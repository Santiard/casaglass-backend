package com.casaglass.casaglass_backend.model;

import com.casaglass.casaglass_backend.config.ProductoDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos")
@org.hibernate.annotations.DynamicUpdate  // ✅ Actualizar solo campos modificados
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonDeserialize(using = ProductoDeserializer.class)  // ✅ Deserializador personalizado
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // � CONTROL DE CONCURRENCIA OPTIMISTA
    // Hibernate incrementa automáticamente este campo en cada UPDATE
    // Si otro proceso modificó el registro, lanza OptimisticLockException
    @Version
    @JsonIgnore  // Ignorar en serialización/deserialización JSON
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

    @Column(nullable = false)
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

    @Lob
    private String descripcion;
}
