package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
@Entity
@Table(name = "ingresos", indexes = {
    @Index(name = "idx_ingreso_fecha", columnList = "fecha"),
    @Index(name = "idx_ingreso_proveedor", columnList = "proveedor_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})   // 👈 evita ruido de proxies
public class Ingreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    @ToString.Exclude
    private Proveedor proveedor;

    @Column(length = 100)
    private String numeroFactura;

    @Column(length = 500)
    private String observaciones;

    @OneToMany(mappedBy = "ingreso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("ingreso-detalles")
    private List<IngresoDetalle> detalles = new ArrayList<>();

    @Column(nullable = false)
    private Double totalCosto = 0.0;

    @Column(nullable = false)
    private Boolean procesado = false; // Indica si ya se actualizó el inventario

    // Método de conveniencia para agregar detalles
    public void agregarDetalle(IngresoDetalle detalle) {
        detalles.add(detalle);
        detalle.setIngreso(this);
    }

    // Método para calcular el total
    public void calcularTotal() {
        this.totalCosto = detalles.stream()
                .mapToDouble(detalle -> detalle.getCostoUnitario() * detalle.getCantidad())
                .sum();
    }
}