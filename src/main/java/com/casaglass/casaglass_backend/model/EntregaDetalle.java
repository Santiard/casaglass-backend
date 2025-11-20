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

    /** Abono específico incluido en la entrega (opcional - solo para órdenes a crédito) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abono_id")
    private Abono abono;

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
            // Si hay un abono específico, usar su monto; si no, usar el monto de la orden
            if (this.abono != null && this.abono.getTotal() != null) {
                this.montoOrden = this.abono.getTotal(); // Monto del abono, no de la orden completa
            } else {
                this.montoOrden = this.orden.getTotal();
            }
            this.numeroOrden = this.orden.getNumero();
            this.fechaOrden = this.orden.getFecha();
            this.ventaCredito = this.orden.isCredito();
            if (this.orden.getCliente() != null) {
                this.clienteNombre = this.orden.getCliente().getNombre();
            }
        }
    }
    
    /** Método para inicializar desde un abono específico */
    public void inicializarDesdeAbono(Abono abono) {
        if (abono != null && abono.getOrden() != null) {
            this.abono = abono;
            this.orden = abono.getOrden();
            this.montoOrden = abono.getTotal(); // Monto del abono
            this.numeroOrden = abono.getNumeroOrden() != null ? abono.getNumeroOrden() : abono.getOrden().getNumero();
            this.fechaOrden = abono.getOrden().getFecha();
            this.ventaCredito = true; // Los abonos siempre son de órdenes a crédito
            if (abono.getCliente() != null) {
                this.clienteNombre = abono.getCliente().getNombre();
            }
        }
    }
}