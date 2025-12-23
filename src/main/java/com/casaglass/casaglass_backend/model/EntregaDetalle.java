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

    /** Reembolso de venta incluido en la entrega (opcional - solo para egresos) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reembolso_venta_id")
    private ReembolsoVenta reembolsoVenta;

    /** Tipo de movimiento (INGRESO para ventas/abonos, EGRESO para reembolsos) */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", length = 20, nullable = false)
    private TipoMovimiento tipoMovimiento = TipoMovimiento.INGRESO;

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

    /** Enumeración para tipo de movimiento en la entrega */
    public enum TipoMovimiento {
        INGRESO,   // Ventas a contado o abonos a créditos (suma)
        EGRESO     // Reembolsos de venta (resta)
    }

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
            this.tipoMovimiento = TipoMovimiento.INGRESO;
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
            this.tipoMovimiento = TipoMovimiento.INGRESO;
            if (abono.getCliente() != null) {
                this.clienteNombre = abono.getCliente().getNombre();
            }
        }
    }

    /** Método para inicializar desde un reembolso de venta (EGRESO) */
    public void inicializarDesdeReembolso(ReembolsoVenta reembolso) {
        if (reembolso != null && reembolso.getOrdenOriginal() != null) {
            this.reembolsoVenta = reembolso;
            this.orden = reembolso.getOrdenOriginal();
            // Monto negativo para representar egreso en cálculos
            this.montoOrden = -Math.abs(reembolso.getTotalReembolso());
            this.numeroOrden = reembolso.getOrdenOriginal().getNumero();
            this.fechaOrden = reembolso.getFecha();
            this.ventaCredito = reembolso.getOrdenOriginal().isCredito();
            this.tipoMovimiento = TipoMovimiento.EGRESO;
            if (reembolso.getCliente() != null) {
                this.clienteNombre = reembolso.getCliente().getNombre();
            }
        }
    }
}