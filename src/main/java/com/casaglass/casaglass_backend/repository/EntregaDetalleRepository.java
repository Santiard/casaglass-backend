package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.EntregaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntregaDetalleRepository extends JpaRepository<EntregaDetalle, Long> {

    List<EntregaDetalle> findByEntregaId(Long entregaId);

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

    long countByEntregaId(Long entregaId);

    void deleteByEntregaId(Long entregaId);
}