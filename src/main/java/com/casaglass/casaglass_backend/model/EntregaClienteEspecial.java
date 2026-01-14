package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro histórico de cierres masivos del cliente especial (ID 499).
 * Cada vez que se marcan créditos especiales como pagados se crea una "entrega" con sus detalles.
 */
@Entity
@Table(name = "entregas_cliente_especial", indexes = {
        @Index(name = "idx_entrega_ce_fecha", columnList = "fecha_registro"),
        @Index(name = "idx_entrega_ce_ejecutado", columnList = "ejecutado_por")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntregaClienteEspecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ejecutado_por", length = 120)
    private String ejecutadoPor;

    @Column(name = "total_creditos", nullable = false)
    private Integer totalCreditos = 0;

    @Column(name = "total_monto_credito", nullable = false)
    private Double totalMontoCredito = 0.0;

    @Column(name = "total_retencion", nullable = false)
    private Double totalRetencion = 0.0;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EntregaClienteEspecialDetalle> detalles = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }
}
