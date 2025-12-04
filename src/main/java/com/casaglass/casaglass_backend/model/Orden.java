package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes", indexes = {
  @Index(name = "idx_orden_numero", columnList = "numero", unique = true),
  @Index(name = "idx_orden_cliente", columnList = "cliente_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Orden {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long numero;

  @NotNull
  private LocalDate fecha;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "cliente_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Cliente cliente;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "sede_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Sede sede;

  // Trabajador encargado de realizar la venta
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "trabajador_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Trabajador trabajador;

  @Column(length = 150)
  private String obra;

  @Lob
  @Column(name = "descripcion", columnDefinition = "TEXT")
  private String descripcion; // Descripción/observaciones adicionales de la orden

  @Column(nullable = false)
  private boolean venta = false;

  @Column(nullable = false)
  private boolean credito = false;

  /** 
   * Crédito asociado a esta orden (si es una venta a crédito)
   * Relación opcional - solo existe si credito = true
   */
  @OneToOne(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JsonIgnoreProperties({"orden", "hibernateLazyInitializer", "handler"})
  private Credito creditoDetalle;

  /**
   * Factura asociada a esta orden (si ya fue facturada)
   * Relación opcional - solo existe si la orden fue facturada
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"orden", "hibernateLazyInitializer", "handler"})
  private Factura factura;

  @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @JsonIgnoreProperties({"orden", "hibernateLazyInitializer", "handler"})
  private List<OrdenItem> items = new ArrayList<>();

  @Column(nullable = false)
  private Double subtotal = 0.0;

  /**
   * Descuentos aplicados a la orden
   */
  @Column(nullable = false)
  private Double descuentos = 0.0;

  @Column(nullable = false)
  private Double total = 0.0;

  @Column(name = "incluida_entrega", nullable = false)
  private boolean incluidaEntrega = false;

  /**
   * Indica si la orden tiene retención de fuente aplicada
   */
  @Column(name = "tiene_retencion_fuente", nullable = false)
  private boolean tieneRetencionFuente = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoOrden estado = EstadoOrden.ACTIVA;

  public enum EstadoOrden {
    ACTIVA, ANULADA
  }
}
