package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "reembolso_ingreso_detalles", indexes = {
    @Index(name = "idx_reembolso_ingreso_detalle_reembolso", columnList = "reembolso_ingreso_id"),
    @Index(name = "idx_reembolso_ingreso_detalle_ingreso", columnList = "ingreso_detalle_id"),
    @Index(name = "idx_reembolso_ingreso_detalle_producto", columnList = "producto_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReembolsoIngresoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reembolso_ingreso_id", nullable = false)
    @ToString.Exclude
    @JsonBackReference("reembolso-ingreso-detalles")
    private ReembolsoIngreso reembolsoIngreso;

    // Detalle original del ingreso que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingreso_detalle_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ingreso"})
    private IngresoDetalle ingresoDetalleOriginal;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto producto;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Double cantidad; // Cantidad a devolver (puede ser parcial)

    @NotNull
    @Column(nullable = false)
    private Double costoUnitario; // Costo unitario al momento del reembolso

    @Column(nullable = false)
    private Double totalLinea; // cantidad * costoUnitario

    // Método para calcular el total de la línea
    @PrePersist
    @PreUpdate
    public void calcularTotalLinea() {
        if (cantidad != null && costoUnitario != null) {
            this.totalLinea = costoUnitario * cantidad;
        }
    }
}

