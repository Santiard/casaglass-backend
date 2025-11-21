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
@Table(name = "reembolsos_venta", indexes = {
    @Index(name = "idx_reembolso_venta_fecha", columnList = "fecha"),
    @Index(name = "idx_reembolso_venta_orden", columnList = "orden_id"),
    @Index(name = "idx_reembolso_venta_cliente", columnList = "cliente_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReembolsoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha; // Fecha del retorno

    // Orden original que se está reembolsando
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "items", "creditoDetalle", "factura"})
    private Orden ordenOriginal;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "sede_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Sede sede;

    @Column(length = 500)
    private String motivo; // Razón del reembolso

    @OneToMany(mappedBy = "reembolsoVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("reembolso-venta-detalles")
    private List<ReembolsoVentaDetalle> detalles = new ArrayList<>();

    @Column(nullable = false)
    private Double subtotal = 0.0;

    @Column(nullable = false)
    private Double descuentos = 0.0; // Descuentos proporcionales

    @Column(nullable = false)
    private Double totalReembolso = 0.0; // Total a reembolsar al cliente

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaReembolso formaReembolso; // Cómo se devuelve el dinero

    public enum FormaReembolso {
        EFECTIVO,
        TRANSFERENCIA,
        NOTA_CREDITO,      // Para aplicar a futuras compras
        AJUSTE_CREDITO     // Si la venta original fue a crédito, ajustar el saldo
    }

    @Column(nullable = false)
    private Boolean procesado = false; // Si ya se actualizó inventario y créditos

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReembolso estado = EstadoReembolso.PENDIENTE;

    public enum EstadoReembolso {
        PENDIENTE,    // Creado pero no procesado
        PROCESADO,    // Inventario actualizado
        ANULADO       // Reembolso cancelado
    }

    // Método de conveniencia para agregar detalles
    public void agregarDetalle(ReembolsoVentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setReembolsoVenta(this);
    }

    // Método para calcular el total
    public void calcularTotal() {
        // Calcular subtotal
        this.subtotal = detalles.stream()
                .mapToDouble(detalle -> detalle.getTotalLinea() != null ? detalle.getTotalLinea() : 0.0)
                .sum();
        
        // Calcular total: subtotal - descuentos
        this.totalReembolso = this.subtotal - (this.descuentos != null ? this.descuentos : 0.0);
    }
}

