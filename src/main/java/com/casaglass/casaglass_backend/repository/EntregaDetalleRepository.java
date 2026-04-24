package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.EntregaDetalle;
import com.casaglass.casaglass_backend.model.EntregaDinero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntregaDetalleRepository extends JpaRepository<EntregaDetalle, Long> {

    @Query("SELECT DISTINCT ed FROM EntregaDetalle ed " +
           "LEFT JOIN FETCH ed.orden o " +
           "LEFT JOIN FETCH ed.abono a " +
           "WHERE ed.entrega.id = :entregaId")
    List<EntregaDetalle> findByEntregaId(@Param("entregaId") Long entregaId);

    List<EntregaDetalle> findByOrdenId(Long ordenId);

    Optional<EntregaDetalle> findByEntregaIdAndOrdenId(Long entregaId, Long ordenId);

    @Query("SELECT ed FROM EntregaDetalle ed WHERE ed.ventaCredito = :credito")
    List<EntregaDetalle> findByTipoVenta(@Param("credito") Boolean credito);

    @Query("SELECT SUM(ed.montoOrden) FROM EntregaDetalle ed WHERE ed.entrega.id = :entregaId")
    Double getTotalMontoByEntrega(@Param("entregaId") Long entregaId);

    @Query("SELECT SUM(ed.montoOrden) FROM EntregaDetalle ed WHERE ed.entrega.id = :entregaId")
    Double calcularMontoTotalPorEntrega(@Param("entregaId") Long entregaId);

    @Query("SELECT ed FROM EntregaDetalle ed WHERE ed.entrega.sede.id = :sedeId")
    List<EntregaDetalle> findBySedeId(@Param("sedeId") Long sedeId);

    @Query("SELECT COUNT(ed) FROM EntregaDetalle ed WHERE ed.orden.id = :ordenId")
    Long countByOrdenId(@Param("ordenId") Long ordenId);

    boolean existsByEntregaIdAndOrdenId(Long entregaId, Long ordenId);

    boolean existsByOrdenIdAndEntregaEstadoIn(Long ordenId, List<EntregaDinero.EstadoEntrega> estados);

    /**
     * Indica si la orden ya está asociada a otra entrega (distinta a {@code excluirEntregaId}) en estado vigente.
     * Necesario para permitir en la <em>misma</em> entrega un ingreso (contado/abono) y un egreso (reembolso) con la misma orden.
     */
    @Query("SELECT CASE WHEN COUNT(ed) > 0 THEN true ELSE false END FROM EntregaDetalle ed " +
           "WHERE ed.orden.id = :ordenId AND ed.entrega.id <> :excluirEntregaId " +
           "AND ed.entrega.estado IN :estados")
    boolean existsByOrdenIdAndEntregaIdNotAndEntrega_EstadoIn(
            @Param("ordenId") Long ordenId,
            @Param("excluirEntregaId") Long excluirEntregaId,
            @Param("estados") List<EntregaDinero.EstadoEntrega> estados);

    Optional<EntregaDetalle> findFirstByOrdenIdAndEntregaEstadoInOrderByIdDesc(Long ordenId, List<EntregaDinero.EstadoEntrega> estados);

    /**
     * Detalles de ingreso (contado o abono) vinculados a una entrega en estado que bloquea edición.
     * Excluye filas de egreso (reembolso) que comparten el mismo {@code orden_id}.
     * Legacy: {@code tipoMovimiento} nulo y sin reembolso = ingreso.
     */
    @Query("SELECT ed FROM EntregaDetalle ed " +
           "WHERE ed.orden.id = :ordenId " +
           "AND ed.entrega.estado IN :estados " +
           "AND (ed.tipoMovimiento = 'INGRESO' OR (ed.tipoMovimiento IS NULL AND ed.reembolsoVenta IS NULL)) " +
           "ORDER BY ed.id DESC")
    List<EntregaDetalle> findDetallesIngresoBloqueoOrden(
            @Param("ordenId") Long ordenId,
            @Param("estados") List<EntregaDinero.EstadoEntrega> estados);

    long countByEntregaId(Long entregaId);

    void deleteByEntregaId(Long entregaId);

    /** True si el abono está referenciado por algún detalle de entrega de dinero (bloquea DELETE en BD). */
    boolean existsByAbono_Id(Long abonoId);

    /**
     * True si ya existe un detalle con este reembolso en <em>otra</em> entrega (no {@code entregaId}).
     */
    @Query("SELECT CASE WHEN COUNT(ed) > 0 THEN true ELSE false END FROM EntregaDetalle ed " +
           "WHERE ed.reembolsoVenta.id = :reembolsoId AND ed.entrega.id <> :entregaId")
    boolean existsByReembolsoVentaIdAndEntregaIdNot(
            @Param("reembolsoId") Long reembolsoId,
            @Param("entregaId") Long entregaId);
}