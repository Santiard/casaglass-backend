package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "reembolso_venta_detalles", indexes = {
    @Index(name = "idx_reembolso_venta_detalle_reembolso", columnList = "reembolso_venta_id"),
    @Index(name = "idx_reembolso_venta_detalle_orden", columnList = "orden_item_id"),
    @Index(name = "idx_reembolso_venta_detalle_producto", columnList = "producto_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReembolsoVentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reembolso_venta_id", nullable = false)
    @ToString.Exclude
    @JsonBackReference("reembolso-venta-detalles")
    private ReembolsoVenta reembolsoVenta;

    // Item original de la orden que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_item_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "orden"})
    private OrdenItem ordenItemOriginal;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto producto;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer cantidad; // Cantidad a devolver (puede ser parcial)

    @NotNull
    @Column(nullable = false)
    private Double precioUnitario; // Precio unitario al momento del reembolso

    @Column(nullable = false)
    private Double totalLinea; // cantidad * precioUnitario

    // Método para calcular el total de la línea
    @PrePersist
    @PreUpdate
    public void calcularTotalLinea() {
        if (cantidad != null && precioUnitario != null) {
            this.totalLinea = precioUnitario * cantidad;
        }
    }
}

