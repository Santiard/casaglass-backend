package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ← CAMBIO
import jakarta.persistence.NamedAttributeNode;              // ← CAMBIO
import jakarta.persistence.NamedEntityGraph;               // ← CAMBIO

@Entity
@Table(
  name = "inventario",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_inventario_producto_sede", columnNames = {"producto_id","sede_id"}
  ),
  indexes = {
    @Index(name = "idx_inv_producto", columnList = "producto_id"),
    @Index(name = "idx_inv_sede", columnList = "sede_id")
  }
)
// Evita problemas al serializar relaciones LAZY en JSON                    ← CAMBIO
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
// Grafo opcional para cargar producto y sede cuando lo necesites (expand) ← CAMBIO
@NamedEntityGraph(
  name = "Inventario.detalle",
  attributeNodes = {
    @NamedAttributeNode("producto"),
    @NamedAttributeNode("sede")
  }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // 🔒 CONTROL DE CONCURRENCIA OPTIMISTA
    // Hibernate incrementa automáticamente este campo en cada UPDATE
    // Si otro proceso modificó el registro, lanza OptimisticLockException
    @Version
    private Long version;

    // Mantén LAZY por defecto; evita ciclos/toString pesados                 ← CAMBIO
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude // evita cargar proxys al hacer toString()              ← CAMBIO
    private Producto producto;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    @ToString.Exclude // evita cargar proxys al hacer toString()              ← CAMBIO
    private Sede sede;

    @Column(nullable = false)
    // Permite valores negativos para manejar ventas anticipadas (productos vendidos antes de tenerlos en tienda)
    // Permite valores decimales para productos medidos en fracciones (ej: vidrios en m²)
    private Double cantidad;
}