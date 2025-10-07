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
    private Double costoUnitario;

    @Column(nullable = false)
    private Double totalLinea;

    // Método para calcular el total de la línea
    @PrePersist
    @PreUpdate
    public void calcularTotalLinea() {
        if (cantidad != null && costoUnitario != null) {
            this.totalLinea = costoUnitario * cantidad;
        }
    }
}