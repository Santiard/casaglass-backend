package com.casaglass.casaglass_backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "traslado_detalles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Schema(name = "TrasladoDetalle", description = "Cuerpo JSON en POST/PUT: use `producto: { \"id\" }` y opcionalmente "
        + "`productoInventarioADescontarSede1: { \"id\" }` (objeto con id) para 1→2/3 y línea corte. "
        + "En el batch, el mismo dato se envía plano como `productoInventarioADescontarSede1Id` (ver TrasladoDetalleBatchDTO).")
public class TrasladoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "traslado_id", nullable = false)
    @JsonBackReference("traslado-detalles")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Traslado traslado;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto producto;

    /**
     * Solo traslados cuya <strong>origen es sede 1 (Insula)</strong> y el destino es 2/3, con línea de
     * {@linkplain com.casaglass.casaglass_backend.model.Corte}:
     * si se informa, se descuenta de inventario <em>normal</em> de sede 1 este producto (p. ej. tubo/entero 6m)
     * por la cantidad de la línea. Si es {@code null}, no se descuenta nada en sede 1 (material visto
     * como corte "sin origen" en Insula). El corte de la línea se acredita en inventario de cortes en destino.
     */
    @Schema(description = "Solo 1→2/3 y línea corte: id del producto entero a descontar en Insula; omitir o null si no aplica descuento de entero")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_inventario_a_descontar_sede1_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto productoInventarioADescontarSede1;

    @Column(nullable = false)
    private Double cantidad;
}
