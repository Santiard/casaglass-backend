package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "entregas_cliente_especial_detalles", indexes = {
        @Index(name = "idx_entrega_ce_det_entrega", columnList = "entrega_id"),
        @Index(name = "idx_entrega_ce_det_credito", columnList = "credito_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntregaClienteEspecialDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id", nullable = false)
    private EntregaClienteEspecial entrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credito_id", nullable = false)
    private Credito credito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(name = "numero_orden")
    private Long numeroOrden;

    @Column(name = "fecha_credito")
    private LocalDate fechaCredito;

    @Column(name = "total_credito", nullable = false)
    private Double totalCredito = 0.0;

    @Column(name = "saldo_anterior", nullable = false)
    private Double saldoAnterior = 0.0;

}
