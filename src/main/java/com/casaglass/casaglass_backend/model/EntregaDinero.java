package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entregas_dinero", indexes = {
    @Index(name = "idx_entrega_sede", columnList = "sede_id"),
    @Index(name = "idx_entrega_empleado", columnList = "empleado_id"),
    @Index(name = "idx_entrega_fecha", columnList = "fecha_entrega"),
    @Index(name = "idx_entrega_estado", columnList = "estado")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntregaDinero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Sede que realiza la entrega */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    /** Empleado que hace la entrega */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Trabajador empleado;

    /** Fecha y hora de la entrega */
    @Column(name = "fecha_entrega", nullable = false)
    @NotNull
    private LocalDate fechaEntrega;

    /** Monto de la entrega (calculado automáticamente desde las órdenes) */
    @Column(name = "monto", nullable = false)
    @NotNull
    @Min(value = 0, message = "Monto no puede ser negativo")
    private Double monto = 0.0;

    /** Desglose por método de pago */
    @Column(name = "monto_efectivo", nullable = false)
    private Double montoEfectivo = 0.0;

    @Column(name = "monto_transferencia", nullable = false)
    private Double montoTransferencia = 0.0;

    @Column(name = "monto_cheque", nullable = false)
    private Double montoCheque = 0.0;

    @Column(name = "monto_deposito", nullable = false)
    private Double montoDeposito = 0.0;

    /**
     * Suma de todas las retenciones en la fuente de los abonos incluidos en esta entrega
     * El montoRetencion de la entrega = SUMA de todos los montoRetencion de los abonos
     */
    @Column(name = "monto_retencion", nullable = false)
    private Double montoRetencion = 0.0;

    /** Modalidad de entrega */
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_entrega", length = 20, nullable = false)
    private ModalidadEntrega modalidadEntrega = ModalidadEntrega.EFECTIVO;

    /** Estado de la entrega */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoEntrega estado = EstadoEntrega.PENDIENTE;

    /** Órdenes incluidas en esta entrega */
    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EntregaDetalle> detalles = new ArrayList<>();

    /** Enumeración para modalidad de entrega */
    public enum ModalidadEntrega {
        EFECTIVO,
        TRANSFERENCIA,
        DEPOSITO,
        CHEQUE,
        MIXTO
    }

    /** Enumeración para estado de la entrega */
    public enum EstadoEntrega {
        PENDIENTE,      // Creada pero no entregada
        ENTREGADA,      // Dinero entregado
        VERIFICADA,     // Administración verificó los montos
        RECHAZADA       // Hay discrepancias que requieren revisión
    }

    /** Método de conveniencia para validar que el desglose coincida con el monto */
    @PrePersist
    @PreUpdate
    public void validarDesglose() {
        Double sumaDesglose = (this.montoEfectivo != null ? this.montoEfectivo : 0.0)
                + (this.montoTransferencia != null ? this.montoTransferencia : 0.0)
                + (this.montoCheque != null ? this.montoCheque : 0.0)
                + (this.montoDeposito != null ? this.montoDeposito : 0.0);
        
        // El monto debe coincidir con la suma del desglose
        if (this.monto != null && Math.abs(sumaDesglose - this.monto) > 0.01) {
            // Si no coincide, ajustar el monto a la suma del desglose
            this.monto = sumaDesglose;
        }
    }
}