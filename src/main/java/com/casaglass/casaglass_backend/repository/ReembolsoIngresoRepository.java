package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.ReembolsoIngreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReembolsoIngresoRepository extends JpaRepository<ReembolsoIngreso, Long> {
    
    // Obtener todos los reembolsos de un ingreso específico
    @Query("SELECT r FROM ReembolsoIngreso r WHERE r.ingresoOriginal.id = :ingresoId ORDER BY r.fecha DESC")
    List<ReembolsoIngreso> findByIngresoOriginalId(@Param("ingresoId") Long ingresoId);
    
    // Obtener todos los reembolsos de un proveedor
    @Query("SELECT r FROM ReembolsoIngreso r WHERE r.proveedor.id = :proveedorId ORDER BY r.fecha DESC")
    List<ReembolsoIngreso> findByProveedorId(@Param("proveedorId") Long proveedorId);
    
    // Obtener reembolso con detalles cargados
    @Query("SELECT r FROM ReembolsoIngreso r LEFT JOIN FETCH r.detalles WHERE r.id = :id")
    Optional<ReembolsoIngreso> findByIdWithDetalles(@Param("id") Long id);
    
    // Obtener todos los reembolsos con detalles cargados
    @Query("SELECT r FROM ReembolsoIngreso r LEFT JOIN FETCH r.detalles ORDER BY r.fecha DESC")
    List<ReembolsoIngreso> findAllWithDetalles();

    /**
     * 🔍 BÚSQUEDA AVANZADA DE REEMBOLSOS DE INGRESO CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     * Nota: sedeId no está implementado actualmente porque Ingreso no tiene campo sede
     */
    @Query("SELECT DISTINCT r FROM ReembolsoIngreso r " +
           "LEFT JOIN FETCH r.detalles d " +
           "LEFT JOIN FETCH r.ingresoOriginal i " +
           "LEFT JOIN FETCH r.proveedor p " +
           "WHERE (:ingresoId IS NULL OR r.ingresoOriginal.id = :ingresoId) AND " +
           "(:proveedorId IS NULL OR r.proveedor.id = :proveedorId) AND " +
           "(:estado IS NULL OR r.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR r.fecha >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR r.fecha <= :fechaHasta) AND " +
           "(:procesado IS NULL OR r.procesado = :procesado) " +
           "ORDER BY r.fecha DESC, r.id DESC")
    List<ReembolsoIngreso> buscarConFiltros(
        @Param("ingresoId") Long ingresoId,
        @Param("proveedorId") Long proveedorId,
        @Param("estado") com.casaglass.casaglass_backend.model.ReembolsoIngreso.EstadoReembolso estado,
        @Param("fechaDesde") java.time.LocalDate fechaDesde,
        @Param("fechaHasta") java.time.LocalDate fechaHasta,
        @Param("procesado") Boolean procesado
    );
}

