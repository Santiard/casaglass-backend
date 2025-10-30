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

    /** Fecha desde la cual se consideran las órdenes para esta entrega */
    @Column(name = "fecha_desde")
    private LocalDate fechaDesde;

    /** Fecha hasta la cual se consideran las órdenes para esta entrega */
    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    /** Monto esperado basado en la suma de órdenes */
    @Column(name = "monto_esperado", nullable = false)
    @NotNull
    @Min(value = 0, message = "Monto esperado no puede ser negativo")
    private Double montoEsperado = 0.0;

    /** Total de gastos operativos de la sede */
    @Column(name = "monto_gastos", nullable = false)
    @NotNull
    @Min(value = 0, message = "Monto gastos no puede ser negativo")
    private Double montoGastos = 0.0;

    /** Monto real entregado (esperado - gastos) */
    @Column(name = "monto_entregado", nullable = false)
    @NotNull
    @Min(value = 0, message = "Monto entregado no puede ser negativo")
    private Double montoEntregado = 0.0;

    /** Desglose por método de pago */
    @Column(name = "monto_efectivo", nullable = false)
    private Double montoEfectivo = 0.0;

    @Column(name = "monto_transferencia", nullable = false)
    private Double montoTransferencia = 0.0;

    @Column(name = "monto_cheque", nullable = false)
    private Double montoCheque = 0.0;

    @Column(name = "monto_deposito", nullable = false)
    private Double montoDeposito = 0.0;

    /** Diferencia entre lo esperado y lo entregado (incluyendo gastos) */
    @Column(name = "diferencia", nullable = false)
    private Double diferencia = 0.0;

    /** Modalidad de entrega */
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_entrega", length = 20, nullable = false)
    private ModalidadEntrega modalidadEntrega = ModalidadEntrega.EFECTIVO;

    /** Estado de la entrega */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoEntrega estado = EstadoEntrega.PENDIENTE;

    /** Observaciones adicionales */
    @Lob
    @Column(name = "observaciones")
    private String observaciones;

    /** Número de comprobante o recibo de entrega */
    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    /** Órdenes incluidas en esta entrega */
    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntregaDetalle> detalles = new ArrayList<>();

    /** Gastos operativos asociados a esta entrega */
    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GastoSede> gastos = new ArrayList<>();

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

    /** Método de conveniencia para calcular automáticamente la diferencia */
    @PrePersist
    @PreUpdate
    public void calcularDiferencia() {
        Double esperado = this.montoEsperado != null ? this.montoEsperado : 0.0;
        Double gastos = this.montoGastos != null ? this.montoGastos : 0.0;
        Double entregado = this.montoEntregado != null ? this.montoEntregado : 0.0;
        
        // Diferencia = (Esperado - Gastos) - Entregado
        Double montoNetoEsperado = esperado - gastos;
        this.diferencia = montoNetoEsperado - entregado;
    }
}