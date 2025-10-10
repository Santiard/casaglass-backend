package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "entrega_detalles", indexes = {
    @Index(name = "idx_detalle_entrega", columnList = "entrega_id"),
    @Index(name = "idx_detalle_orden", columnList = "orden_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntregaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Entrega a la que pertenece este detalle */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id", nullable = false)
    private EntregaDinero entrega;

    /** Orden incluida en la entrega */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    /** Monto de la orden al momento de la entrega (snapshot para auditoría) */
    @Column(name = "monto_orden", nullable = false)
    @NotNull
    @Min(value = 0, message = "Monto orden no puede ser negativo")
    private Double montoOrden;

    /** Número de orden (snapshot para reportes) */
    @Column(name = "numero_orden")
    private Long numeroOrden;

    /** Fecha de la orden (snapshot) */
    @Column(name = "fecha_orden")
    private LocalDate fechaOrden;

    /** Indica si la venta fue a crédito */
    @Column(name = "venta_credito")
    private Boolean ventaCredito = false;

    /** Cliente de la orden (snapshot) */
    @Column(name = "cliente_nombre", length = 100)
    private String clienteNombre;

    /** Observaciones específicas de esta orden en la entrega */
    @Column(name = "observaciones", length = 255)
    private String observaciones;

    /** Método para inicializar campos snapshot desde la orden */
    public void inicializarDesdeOrden() {
        if (this.orden != null) {
            this.montoOrden = this.orden.getTotal();
            this.numeroOrden = this.orden.getNumero();
            this.fechaOrden = this.orden.getFecha();
            this.ventaCredito = this.orden.isCredito();
            if (this.orden.getCliente() != null) {
                this.clienteNombre = this.orden.getCliente().getNombre();
            }
        }
    }
}