package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "orden_items", indexes = {
  @Index(name = "idx_item_orden", columnList = "orden_id")
})
@Data @NoArgsConstructor @AllArgsConstructor
public class OrdenItem {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "orden_id", nullable = false)
  private Orden orden;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "producto_id")
  private Producto producto;           // referencia al catálogo

  @Column(length = 200)
  private String descripcion;          // nombre/medidas “congeladas”

  @Min(1)
  @Column(nullable = false)
  private Integer cantidad;

  @Column(nullable = false)
  private Double precioUnitario;

  @Column(nullable = false)
  private Double totalLinea;
}