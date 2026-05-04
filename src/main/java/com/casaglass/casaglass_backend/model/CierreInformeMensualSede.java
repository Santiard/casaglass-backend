package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "cierre_informe_mensual_sede",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cierre_sede_anio_mes",
                columnNames = {"sede_id", "anio", "mes"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CierreInformeMensualSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false)
    private Integer anio;

    /** 1–12 */
    @Column(nullable = false)
    private Integer mes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInforme estado = EstadoInforme.CERRADO;

    @Column(name = "ventas_mes")
    private Double ventasMes;

    @Column(name = "dinero_recogido_mes")
    private Double dineroRecogidoMes;

    @Column(name = "deudas_mes")
    private Double deudasMes;

    @Column(name = "deudas_activas_totales")
    private Double deudasActivasTotales;

    @Column(name = "valor_inventario")
    private Double valorInventario;

    @Column(name = "orden_numero_min")
    private Long ordenNumeroMin;

    @Column(name = "orden_numero_max")
    private Long ordenNumeroMax;

    @Column(name = "cantidad_ordenes_ventas_mes")
    private Integer cantidadOrdenesVentasMes;

    public enum EstadoInforme {
        BORRADOR,
        CERRADO
    }
}
