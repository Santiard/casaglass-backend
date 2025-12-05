package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "facturas", indexes = {
  @Index(name = "idx_factura_numero", columnList = "numero_factura", unique = true),
  @Index(name = "idx_factura_orden", columnList = "orden_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Factura {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Número de factura público (generado automáticamente)
   * Formato: Factura electrónica o manual
   * Diferente del ID interno de la base de datos
   */
  @Column(name = "numero_factura", nullable = false, unique = true)
  private String numeroFactura;

  /**
   * Fecha de emisión de la factura
   */
  @NotNull
  @Column(nullable = false)
  private LocalDate fecha;

  /**
   * Orden que se está facturando (relación 1 a 1)
   * Solo una factura por orden
   */
  @OneToOne
  @JoinColumn(name = "orden_id", nullable = false, unique = true)
  @NotNull
  @JsonIgnoreProperties({"factura", "hibernateLazyInitializer", "handler"})
  private Orden orden;

  /**
   * Cliente al que se factura (opcional)
   * Si no se especifica, se usa el cliente de la orden
   * Permite facturar a un cliente diferente al de la orden original
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "cliente_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Cliente cliente;

  // ========== CAMPOS FINANCIEROS ==========

  /**
   * Subtotal (sin impuestos)
   */
  @NotNull
  @Positive
  @Column(nullable = false)
  private Double subtotal;

  /**
   * Descuentos aplicados
   */
  @Column(nullable = false)
  private Double descuentos = 0.0;

  /**
   * IVA (Impuesto sobre el Valor Agregado)
   * Valor monetario del IVA incluido en el subtotal
   * NOTA: El subtotal ya incluye IVA, este campo solo se usa para registro/contabilidad
   */
  @Column(nullable = false)
  private Double iva = 0.0;

  /**
   * Retención en la fuente
   * Impuesto retenido del cliente
   */
  @Column(nullable = false)
  private Double retencionFuente = 0.0;

  /**
   * Total final de la factura
   * Fórmula: (subtotal - descuentos) - retencionFuente
   * NOTA: El subtotal ya incluye IVA, por lo que NO se suma el IVA al total
   */
  @NotNull
  @Positive
  @Column(nullable = false)
  private Double total;

  /**
   * Forma de pago
   */
  @Column(length = 50)
  private String formaPago; // EFECTIVO, TRANSFERENCIA, CHEQUE, etc.

  /**
   * Observaciones adicionales
   */
  @Column(length = 500)
  private String observaciones;

  /**
   * Estado de la factura
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoFactura estado = EstadoFactura.PENDIENTE;

  /**
   * Fecha de pago (si aplica)
   */
  private LocalDate fechaPago;

  public enum EstadoFactura {
    PENDIENTE,      // Factura generada pero no pagada
    PAGADA,         // Factura pagada completamente
    ANULADA,        // Factura anulada
    EN_PROCESO      // Factura en proceso de pago (parcial)
  }

  /**
   * Método helper para calcular el total automáticamente
   * NOTA: El subtotal ya incluye IVA, por lo que NO se suma el IVA al total
   * Fórmula: total = (subtotal - descuentos) - retencionFuente
   */
  public void calcularTotal() {
    double baseImponible = subtotal - descuentos;
    // El subtotal ya incluye IVA, solo se resta la retención de fuente
    double totalCalculado = baseImponible - retencionFuente;
    // Redondear a 2 decimales
    this.total = Math.round(totalCalculado * 100.0) / 100.0;
  }
}

