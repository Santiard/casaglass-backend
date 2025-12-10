package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "ingreso_detalles", indexes = {
    @Index(name = "idx_ingreso_detalle_ingreso", columnList = "ingreso_id"),
    @Index(name = "idx_ingreso_detalle_producto", columnList = "producto_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})   
public class IngresoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ingreso_id", nullable = false)
    @ToString.Exclude
    @JsonBackReference("ingreso-detalles")  
    private Ingreso ingreso;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude
    private Producto producto;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer cantidad;

    @NotNull
    @Column(nullable = false)
    private Double costoUnitario; // Costo original del ingreso (para calcular totalCosto y trazabilidad)

    @NotNull
    @Column(nullable = false)
    private Double costoUnitarioPonderado; // Costo calculado con promedio ponderado (viene del frontend, se usa para actualizar producto.costo)

    @Column(nullable = false)
    private Double totalLinea; // Se calcula con costoUnitario (costo original)

    // Método para calcular el total de la línea
    // IMPORTANTE: Siempre usa costoUnitario (costo original) para calcular totalLinea
    // El totalLinea refleja lo que realmente se pagó en el ingreso
    @PrePersist
    @PreUpdate
    public void calcularTotalLinea() {
        if (cantidad != null && costoUnitario != null) {
            // totalLinea siempre se calcula con costoUnitario (costo original)
            this.totalLinea = costoUnitario * cantidad;
        }
    }
}