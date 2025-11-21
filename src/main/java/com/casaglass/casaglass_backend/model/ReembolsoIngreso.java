package com.casaglass.casaglass_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reembolsos_ingreso", indexes = {
    @Index(name = "idx_reembolso_ingreso_fecha", columnList = "fecha"),
    @Index(name = "idx_reembolso_ingreso_ingreso", columnList = "ingreso_id"),
    @Index(name = "idx_reembolso_ingreso_proveedor", columnList = "proveedor_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReembolsoIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha; // Fecha del retorno

    // Ingreso original que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingreso_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "detalles"})
    private Ingreso ingresoOriginal;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "proveedor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Proveedor proveedor;

    @Column(length = 100)
    private String numeroFacturaDevolucion; // Factura de devolución del proveedor (opcional)

    @Column(length = 500)
    private String motivo; // Razón del reembolso

    @OneToMany(mappedBy = "reembolsoIngreso", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("reembolso-ingreso-detalles")
    private List<ReembolsoIngresoDetalle> detalles = new ArrayList<>();

    @Column(nullable = false)
    private Double totalReembolso = 0.0; // Total a reembolsar al proveedor

    @Column(nullable = false)
    private Boolean procesado = false; // Si ya se actualizó el inventario

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReembolso estado = EstadoReembolso.PENDIENTE;

    public enum EstadoReembolso {
        PENDIENTE,    // Creado pero no procesado
        PROCESADO,    // Inventario actualizado
        ANULADO       // Reembolso cancelado
    }

    // Método de conveniencia para agregar detalles
    public void agregarDetalle(ReembolsoIngresoDetalle detalle) {
        detalles.add(detalle);
        detalle.setReembolsoIngreso(this);
    }

    // Método para calcular el total
    public void calcularTotal() {
        this.totalReembolso = detalles.stream()
                .mapToDouble(detalle -> detalle.getTotalLinea() != null ? detalle.getTotalLinea() : 0.0)
                .sum();
    }
}

