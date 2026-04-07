package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_cortes_plan", indexes = {
    @Index(name = "idx_orden_corte_plan_orden", columnList = "orden_id"),
    @Index(name = "idx_orden_corte_plan_estado", columnList = "estado")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrdenCortePlan {

    public enum EstadoPlanCorte {
        PLANIFICADO,
        EJECUTADO,
        ANULADO
    }

    public enum OrigenTipo {
        PERFIL,
        CORTE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_origen_id", nullable = false)
    private Producto productoOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 20)
    private OrigenTipo origenTipo = OrigenTipo.PERFIL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_corte_id")
    private Corte origenCorte;

    @Column(name = "plan_orden", nullable = false)
    private Integer planOrden = 0;

    @Column(name = "medida_solicitada", nullable = false)
    private Integer medidaSolicitada;

    @Column(name = "medida_sobrante")
    private Integer medidaSobrante;

    @Column(nullable = false)
    private Double cantidad;

    @Column(name = "precio_unitario_solicitado")
    private Double precioUnitarioSolicitado;

    @Column(name = "precio_unitario_sobrante")
    private Double precioUnitarioSobrante;

    @Column(name = "reutilizar_corte_id")
    private Long reutilizarCorteId;

    @Column(name = "corte_solicitado_id")
    private Long corteSolicitadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPlanCorte estado = EstadoPlanCorte.PLANIFICADO;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenCortePlanSede> cantidadesPorSede = new ArrayList<>();
}
