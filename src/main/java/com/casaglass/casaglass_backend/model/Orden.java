package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes", indexes = {
  @Index(name = "idx_orden_numero", columnList = "numero", unique = true),
  @Index(name = "idx_orden_cliente", columnList = "cliente_id")
})
@Data @NoArgsConstructor @AllArgsConstructor
public class Orden {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long numero;                 // numerador compartido

  @NotNull
  private LocalDateTime fecha;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "sede_id", nullable = false)
  private Sede sede;

  @Column(length = 150)
  private String obra;

  @Column(nullable = false)
  private boolean venta = false;       // ← true=VENTA, false=COTIZACION

  @Column(nullable = false)
  private boolean credito = false;     // ← true=es a crédito

  @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrdenItem> items = new ArrayList<>();

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal total = BigDecimal.ZERO;
}