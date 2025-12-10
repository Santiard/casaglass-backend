package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Traslado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

    List<Traslado> findBySedeOrigenId(Long sedeOrigenId);

    List<Traslado> findBySedeDestinoId(Long sedeDestinoId);

    List<Traslado> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Traslado> findBySedeOrigenIdAndSedeDestinoId(Long sedeOrigenId, Long sedeDestinoId);

    // 🔁 Query optimizada para cargar todas las relaciones y evitar LazyInitializationException
    @Query("""
        SELECT DISTINCT t FROM Traslado t 
        LEFT JOIN FETCH t.sedeOrigen 
        LEFT JOIN FETCH t.sedeDestino 
        LEFT JOIN FETCH t.trabajadorConfirmacion 
        LEFT JOIN FETCH t.detalles d 
        LEFT JOIN FETCH d.producto p 
        LEFT JOIN FETCH p.categoria
        ORDER BY t.fecha DESC, t.id DESC
        """)
    List<Traslado> findAllWithDetails();

    // 🆕 Métodos adicionales para el servicio de movimientos
    List<Traslado> findBySedeOrigenIdOrSedeDestinoId(Long sedeOrigenId, Long sedeDestinoId);

    List<Traslado> findByTrabajadorConfirmacionIsNull();

    List<Traslado> findByTrabajadorConfirmacionIsNotNull();
    
    // 📊 MÉTODOS PARA DASHBOARD - TRASLADOS PENDIENTES
    @Query("""
        SELECT DISTINCT t FROM Traslado t 
        LEFT JOIN FETCH t.sedeOrigen 
        LEFT JOIN FETCH t.sedeDestino 
        LEFT JOIN FETCH t.detalles 
        WHERE (t.sedeOrigen.id = :sedeId OR t.sedeDestino.id = :sedeId) 
        AND t.fechaConfirmacion IS NULL 
        ORDER BY t.fecha ASC
        """)
    List<Traslado> findTrasladosPendientesBySede(Long sedeId);
    
    List<Traslado> findBySedeOrigenIdAndFechaConfirmacionIsNull(Long sedeOrigenId);
    
    List<Traslado> findBySedeDestinoIdAndFechaConfirmacionIsNull(Long sedeDestinoId);

    /**
     * 🔍 BÚSQUEDA AVANZADA DE TRASLADOS CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     * Nota: estado se maneja como "PENDIENTE" (fechaConfirmacion IS NULL) o "CONFIRMADO" (fechaConfirmacion IS NOT NULL)
     */
    @Query("SELECT DISTINCT t FROM Traslado t " +
           "LEFT JOIN FETCH t.sedeOrigen " +
           "LEFT JOIN FETCH t.sedeDestino " +
           "LEFT JOIN FETCH t.trabajadorConfirmacion " +
           "WHERE (:sedeOrigenId IS NULL OR t.sedeOrigen.id = :sedeOrigenId) AND " +
           "(:sedeDestinoId IS NULL OR t.sedeDestino.id = :sedeDestinoId) AND " +
           "(:sedeId IS NULL OR t.sedeOrigen.id = :sedeId OR t.sedeDestino.id = :sedeId) AND " +
           "(:fechaDesde IS NULL OR t.fecha >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR t.fecha <= :fechaHasta) AND " +
           "(:confirmado IS NULL OR (:confirmado = true AND t.fechaConfirmacion IS NOT NULL) OR (:confirmado = false AND t.fechaConfirmacion IS NULL)) AND " +
           "(:trabajadorId IS NULL OR t.trabajadorConfirmacion.id = :trabajadorId) " +
           "ORDER BY t.fecha DESC, t.id DESC")
    List<Traslado> buscarConFiltros(
        @Param("sedeOrigenId") Long sedeOrigenId,
        @Param("sedeDestinoId") Long sedeDestinoId,
        @Param("sedeId") Long sedeId,
        @Param("fechaDesde") java.time.LocalDate fechaDesde,
        @Param("fechaHasta") java.time.LocalDate fechaHasta,
        @Param("confirmado") Boolean confirmado,
        @Param("trabajadorId") Long trabajadorId
    );
}