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

    /** Método de pago (TF = transferencia). Puedes usar enum o texto libre; aquí enum: */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20, nullable = false)
    private MetodoPago metodoPago = MetodoPago.TRANSFERENCIA;

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

    public enum MetodoPago {
        EFECTIVO, TRANSFERENCIA, TARJETA, CHEQUE, OTRO
    }
}