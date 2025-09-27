package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "creditos",
       indexes = {
         @Index(name = "idx_credito_cliente", columnList = "cliente_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Credito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Un crédito por cliente (si quieres permitir varios por cliente, cambia a ManyToOne) */
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    /** Órdenes asociadas al crédito (cada orden pertenece a lo sumo a un crédito) */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "credito_ordenes",
        joinColumns = @JoinColumn(name = "credito_id"),
        inverseJoinColumns = @JoinColumn(name = "orden_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_credito_orden_unica", columnNames = { "orden_id" })
    )
    private List<Orden> ordenes = new ArrayList<>();

    /** Total de la deuda (puedes mantenerlo sincronizado desde el servicio) */
    @NotNull
    @Column(nullable = false)
    private Double totalDeuda = 0.0;

    /** Historial de abonos aplicados a este crédito */
    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Abono> abonos = new ArrayList<>();
}