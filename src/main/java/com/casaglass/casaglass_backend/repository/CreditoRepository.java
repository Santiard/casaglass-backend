package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Credito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CreditoRepository extends JpaRepository<Credito, Long> {
    
    // Búsqueda por cliente (para el nuevo modelo permite múltiples créditos por cliente)
    List<Credito> findByClienteId(Long clienteId);
    
    // Búsqueda por orden (en el nuevo modelo cada crédito tiene una orden específica)
    Optional<Credito> findByOrdenId(Long ordenId);
    
    // Búsqueda por estado
    List<Credito> findByEstado(Credito.EstadoCredito estado);
    
    // Métodos adicionales para consultas específicas
    List<Credito> findByClienteIdAndEstado(Long clienteId, Credito.EstadoCredito estado);
    
    // 💰 MÉTODO ESPECIALIZADO PARA PÁGINA DE ABONOS - Créditos pendientes con saldo > 0
    List<Credito> findByClienteIdAndEstadoAndSaldoPendienteGreaterThan(Long clienteId, Credito.EstadoCredito estado, Double saldoPendiente);
    
    // Para compatibilidad con código anterior (método único)
    Optional<Credito> findFirstByClienteId(Long clienteId);
    
    // 📊 MÉTODO PARA DASHBOARD - CRÉDITOS POR SEDE
    List<Credito> findByOrdenSedeIdAndEstado(Long sedeId, Credito.EstadoCredito estado);

    // 📊 MÉTODO PARA DASHBOARD - DEUDAS CREADAS EN EL MES (sin importar estado)
    List<Credito> findByOrdenSedeIdAndFechaInicioBetween(Long sedeId, LocalDate desde, LocalDate hasta);

    // 📊 MÉTODO PARA DASHBOARD - HISTÓRICO TOTAL DE DEUDAS DE LA SEDE (todos los estados)
    List<Credito> findByOrdenSedeId(Long sedeId);

    /**
     * Encuentra créditos por rango de fecha de inicio
     */
    List<Credito> findByFechaInicioBetween(LocalDate fechaDesde, LocalDate fechaHasta);
    /**
     * 🔍 BÚSQUEDA AVANZADA DE CRÉDITOS CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     * Nota: fechaDesde y fechaHasta se aplican a fechaInicio del crédito
     */
    /**
     * 🚫 EXCLUYE al cliente especial (ID 499 - JAIRO JAVIER VELANDIA)
     * Usar para listados normales de créditos
     */
    @Query("SELECT DISTINCT c FROM Credito c " +
           "LEFT JOIN FETCH c.cliente cl " +
           "LEFT JOIN FETCH c.orden o " +
           "LEFT JOIN FETCH o.sede s " +
           "WHERE c.cliente.id != 499 AND " + // ⚠️ EXCLUIR CLIENTE ESPECIAL
           "(:clienteId IS NULL OR c.cliente.id = :clienteId) AND " +
           "(:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
           "(:estado IS NULL OR c.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR c.fechaInicio >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR c.fechaInicio <= :fechaHasta) " +
           "ORDER BY c.fechaInicio DESC, c.id DESC")
    List<Credito> buscarConFiltros(
        @Param("clienteId") Long clienteId,
        @Param("sedeId") Long sedeId,
        @Param("estado") Credito.EstadoCredito estado,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta
    );
    
    /**
     * ⭐ SOLO créditos del cliente especial (ID 499 - JAIRO JAVIER VELANDIA)
     * Usar para el módulo dedicado de este cliente
     */
    @Query("SELECT DISTINCT c FROM Credito c " +
           "LEFT JOIN FETCH c.cliente cl " +
           "LEFT JOIN FETCH c.orden o " +
           "LEFT JOIN FETCH o.sede s " +
           "WHERE c.cliente.id = 499 AND " + // ✅ SOLO CLIENTE ESPECIAL
           "(:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
           "(:estado IS NULL OR c.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR c.fechaInicio >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR c.fechaInicio <= :fechaHasta) " +
           "ORDER BY c.fechaInicio DESC, c.id DESC")
    List<Credito> buscarClienteEspecial(
        @Param("sedeId") Long sedeId,
        @Param("estado") Credito.EstadoCredito estado,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta
    );
}