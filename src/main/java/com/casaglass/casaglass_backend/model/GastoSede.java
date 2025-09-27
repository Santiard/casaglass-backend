package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gastos_sede", indexes = {
    @Index(name = "idx_gasto_entrega", columnList = "entrega_id"),
    @Index(name = "idx_gasto_sede", columnList = "sede_id"),
    @Index(name = "idx_gasto_fecha", columnList = "fecha_gasto"),
    @Index(name = "idx_gasto_tipo", columnList = "tipo")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GastoSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Entrega asociada (opcional - puede ser un gasto independiente) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id")
    private EntregaDinero entrega;

    /** Sede donde se realizó el gasto */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    /** Fecha en que se realizó el gasto */
    @Column(name = "fecha_gasto", nullable = false)
    @NotNull
    private LocalDateTime fechaGasto;

    /** Monto del gasto */
    @Column(name = "monto", nullable = false)
    @NotNull
    @Min(value = 1, message = "Monto debe ser mayor a 0")
    private Double monto;

    /** Concepto del gasto (categoría general) */
    @Column(name = "concepto", length = 100, nullable = false)
    @NotBlank
    private String concepto;

    /** Descripción detallada del gasto */
    @Lob
    @Column(name = "descripcion")
    private String descripcion;

    /** Número de comprobante, factura o recibo */
    @Column(name = "comprobante", length = 50)
    private String comprobante;

    /** Tipo de gasto */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoGasto tipo = TipoGasto.OPERATIVO;

    /** Empleado que autorizó o realizó el gasto */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Trabajador empleado;

    /** Proveedor del gasto (si aplica) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    /** Indica si el gasto fue aprobado */
    @Column(name = "aprobado")
    private Boolean aprobado = false;

    /** Observaciones adicionales */
    @Column(name = "observaciones", length = 255)
    private String observaciones;

    /** Enumeración para tipos de gasto */
    public enum TipoGasto {
        OPERATIVO,        // Limpieza, papelería, etc.
        MANTENIMIENTO,    // Reparaciones, mantenimiento
        COMBUSTIBLE,      // Gasolina, transporte
        SERVICIOS,        // Agua, luz, internet
        EMERGENCIA,       // Gastos urgentes no previstos
        ALIMENTACION,     // Almuerzos, refrigerios
        OTRO             // Otros gastos
    }
}