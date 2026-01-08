package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(
    name = "inventario_cortes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inventario_corte_sede", columnNames = {"corte_id","sede_id"}
    ),
    indexes = {
        @Index(name = "idx_inv_corte", columnList = "corte_id"),
        @Index(name = "idx_inv_corte_sede", columnList = "sede_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InventarioCorte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "corte_id", nullable = false)
    private Corte corte;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false)
    @Min(0)
    private Double cantidad;
}