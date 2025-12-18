package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "abonos",
       indexes = {
         @Index(name = "idx_abono_credito", columnList = "credito_id"),
         @Index(name = "idx_abono_orden", columnList = "orden_id"),
         @Index(name = "idx_abono_cliente", columnList = "cliente_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Abono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** A qué crédito aplica el abono (historial) */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "credito_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Credito credito;

    /** (Opcional) Orden específica a la que imputas el abono */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Orden orden;

    /** Snapshot del número de orden (para reportes/impresión incluso si se borra la orden) */
    @Column(name = "numero_orden")
    private Long numeroOrden;

    /** Cliente (redundante con credito.cliente; mantenlo consistente desde el servicio) */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    @NotNull
    private LocalDate fecha;

    /** Método de pago (texto libre: EFECTIVO, TRANSFERENCIA, TARJETA, CHEQUE, OTRO, etc.)
     *  Puede incluir descripciones detalladas con múltiples métodos, retenciones y observaciones */
    @Column(name = "metodo_pago", length = 3000, nullable = false)
    private String metodoPago = "TRANSFERENCIA";

    /**
     * 💰 MONTOS POR MÉTODO DE PAGO
     * Almacenamiento numérico estructurado para cálculos exactos y auditoría
     * La suma de efectivo + transferencia + cheque DEBE igualar el total del abono
     */
    @Column(name = "monto_efectivo", nullable = false)
    private Double montoEfectivo = 0.0;

    @Column(name = "monto_transferencia", nullable = false)
    private Double montoTransferencia = 0.0;

    @Column(name = "monto_cheque", nullable = false)
    private Double montoCheque = 0.0;

    /**
     * Monto de retención en la fuente aplicado en ESTE abono específico
     * NO se suma a los métodos de pago (es informativa/contable)
     */
    @Column(name = "monto_retencion", nullable = false)
    private Double montoRetencion = 0.0;

    /** Número de factura/recibo/soporte del abono */
    @Column(name = "factura", length = 50)
    private String factura;

    /** Monto del abono */
    @NotNull
    @Column(nullable = false)
    private Double total;

    /** Saldo del crédito después de aplicar este abono (snapshot para auditoría) */
    @NotNull
    @Column(nullable = false)
    private Double saldo;
}