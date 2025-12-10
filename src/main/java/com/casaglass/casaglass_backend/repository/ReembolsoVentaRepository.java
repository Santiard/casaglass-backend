package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.ReembolsoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReembolsoVentaRepository extends JpaRepository<ReembolsoVenta, Long> {
    
    // Obtener todos los reembolsos de una orden específica
    @Query("SELECT r FROM ReembolsoVenta r WHERE r.ordenOriginal.id = :ordenId ORDER BY r.fecha DESC")
    List<ReembolsoVenta> findByOrdenOriginalId(@Param("ordenId") Long ordenId);
    
    // Obtener todos los reembolsos de un cliente
    @Query("SELECT r FROM ReembolsoVenta r WHERE r.cliente.id = :clienteId ORDER BY r.fecha DESC")
    List<ReembolsoVenta> findByClienteId(@Param("clienteId") Long clienteId);
    
    // Obtener reembolso con detalles cargados
    @Query("SELECT r FROM ReembolsoVenta r LEFT JOIN FETCH r.detalles WHERE r.id = :id")
    Optional<ReembolsoVenta> findByIdWithDetalles(@Param("id") Long id);
    
    // Obtener todos los reembolsos con detalles cargados
    @Query("SELECT r FROM ReembolsoVenta r LEFT JOIN FETCH r.detalles ORDER BY r.fecha DESC")
    List<ReembolsoVenta> findAllWithDetalles();

    /**
     * 🔍 BÚSQUEDA AVANZADA DE REEMBOLSOS DE VENTA CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     */
    @Query("SELECT DISTINCT r FROM ReembolsoVenta r " +
           "LEFT JOIN FETCH r.detalles d " +
           "LEFT JOIN FETCH r.ordenOriginal o " +
           "LEFT JOIN FETCH r.cliente c " +
           "LEFT JOIN FETCH r.sede s " +
           "WHERE (:ordenId IS NULL OR r.ordenOriginal.id = :ordenId) AND " +
           "(:clienteId IS NULL OR r.cliente.id = :clienteId) AND " +
           "(:sedeId IS NULL OR r.sede.id = :sedeId) AND " +
           "(:estado IS NULL OR r.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR r.fecha >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR r.fecha <= :fechaHasta) AND " +
           "(:procesado IS NULL OR r.procesado = :procesado) " +
           "ORDER BY r.fecha DESC, r.id DESC")
    List<ReembolsoVenta> buscarConFiltros(
        @Param("ordenId") Long ordenId,
        @Param("clienteId") Long clienteId,
        @Param("sedeId") Long sedeId,
        @Param("estado") com.casaglass.casaglass_backend.model.ReembolsoVenta.EstadoReembolso estado,
        @Param("fechaDesde") java.time.LocalDate fechaDesde,
        @Param("fechaHasta") java.time.LocalDate fechaHasta,
        @Param("procesado") Boolean procesado
    );
}

