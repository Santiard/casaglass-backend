package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "orden_items", indexes = {
  @Index(name = "idx_item_orden", columnList = "orden_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "orden_id", nullable = false)
  @JsonIgnoreProperties({"items", "hibernateLazyInitializer", "handler"})
  private Orden orden;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "producto_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Producto producto;

  @Column(length = 200)
  private String descripcion;

  @Min(1)
  @Column(nullable = false)
  private Integer cantidad;

  @Column(nullable = false)
  private Double precioUnitario;

  @Column(nullable = false)
  private Double totalLinea;
}
