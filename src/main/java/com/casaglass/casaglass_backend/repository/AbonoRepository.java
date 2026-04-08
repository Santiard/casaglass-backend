package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AbonoRepository extends JpaRepository<Abono, Long> {
    List<Abono> findByCreditoId(Long creditoId);

    List<Abono> findByClienteId(Long clienteId);

    /**
     * Encuentra abonos de un cliente en un rango de fechas
     * Optimizado para mejorar rendimiento al filtrar en la base de datos
     */
    List<Abono> findByClienteIdAndFechaBetween(Long clienteId, LocalDate fechaDesde, LocalDate fechaHasta);

    List<Abono> findByOrdenId(Long ordenId);

    /**
     * Encuentra todos los abonos en un rango de fechas
     */
    List<Abono> findByFechaBetween(LocalDate fechaDesde, LocalDate fechaHasta);
    /**
     * Encuentra abonos de una orden específica en un rango de fechas
     * Usado para cálculos de entregas de dinero
     */
    List<Abono> findByOrdenIdAndFechaBetween(Long ordenId, LocalDate fechaDesde, LocalDate fechaHasta);

    /**
     * Calcula el total de abonos de una orden en un período
     */
    @Query("SELECT COALESCE(SUM(a.total), 0.0) FROM Abono a WHERE a.orden.id = :ordenId AND a.fecha BETWEEN :fechaDesde AND :fechaHasta")
    Double calcularTotalAbonosOrdenEnPeriodo(@Param("ordenId") Long ordenId, 
                                           @Param("fechaDesde") LocalDate fechaDesde, 
                                           @Param("fechaHasta") LocalDate fechaHasta);
    
    /**
     * 💰 ABONOS DISPONIBLES PARA ENTREGA
     * - De órdenes de la sede especificada
     * - Con fecha de abono en el período
     * - De órdenes a crédito (credito = true)
     * - De órdenes ACTIVAS
     * - Que NO estén ya incluidos en otra entrega (verificado por LEFT JOIN con EntregaDetalle)
     * 
     * NOTA: No se filtra por estado del crédito porque un abono debe aparecer aunque
     * el crédito se haya cerrado después, ya que el abono fue realizado en el período
     * y necesita ser entregado.
     */
    /**
     * 💰 ABONOS DISPONIBLES PARA ENTREGA
     * ⚠️ EXCLUYE abonos del cliente especial (ID 499 - JAIRO JAVIER VELANDIA)
     * ✅ Usa la sede del ABONO (donde se registró el pago), no la sede de la orden
     */
    @Query("SELECT DISTINCT a FROM Abono a " +
           "JOIN a.orden o " +
           "LEFT JOIN EntregaDetalle ed ON ed.abono.id = a.id WHERE " +
           "a.cliente.id != 499 AND " + // ⚠️ EXCLUIR CLIENTE ESPECIAL
           "a.sede.id = :sedeId AND " + // ✅ Sede donde se registró el abono (donde se recibió el pago)
           "a.fecha BETWEEN :fechaDesde AND :fechaHasta AND " +
           "o.credito = true AND " +
           "o.venta = true AND " +
           "o.estado = 'ACTIVA' AND " +
           "ed.id IS NULL")
    List<Abono> findAbonosDisponiblesParaEntrega(
        @Param("sedeId") Long sedeId,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta
    );

    /**
     * 💰 ABONOS DISPONIBLES PARA ENTREGA (SIN FILTRO DE FECHA)
     * Trae todos los abonos pendientes de la sede que no estén incluidos en otra entrega.
     */
    @Query("SELECT DISTINCT a FROM Abono a " +
           "JOIN a.orden o " +
           "LEFT JOIN EntregaDetalle ed ON ed.abono.id = a.id WHERE " +
           "a.cliente.id != 499 AND " +
           "a.sede.id = :sedeId AND " +
           "o.credito = true AND " +
           "o.venta = true AND " +
           "o.estado = 'ACTIVA' AND " +
           "ed.id IS NULL")
    List<Abono> findAbonosDisponiblesParaEntregaSinFecha(
        @Param("sedeId") Long sedeId
    );

    /**
     * 🔍 BÚSQUEDA AVANZADA DE ABONOS CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     * ✅ Usa la sede del ABONO (donde se registró el pago), no la sede de la orden
     */
    @Query("SELECT DISTINCT a FROM Abono a " +
           "LEFT JOIN a.credito c " +
           "LEFT JOIN a.orden o " +
           "WHERE (:clienteId IS NULL OR a.cliente.id = :clienteId) AND " +
           "(:creditoId IS NULL OR a.credito.id = :creditoId) AND " +
           "(:fechaDesde IS NULL OR a.fecha >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR a.fecha <= :fechaHasta) AND " +
           "(:metodoPago IS NULL OR LOWER(a.metodoPago) LIKE LOWER(CONCAT('%', :metodoPago, '%'))) AND " +
           "(:sedeId IS NULL OR a.sede.id = :sedeId) " + // ✅ Sede donde se registró el abono
           "ORDER BY a.fecha DESC, a.id DESC")
    List<Abono> buscarConFiltros(
        @Param("clienteId") Long clienteId,
        @Param("creditoId") Long creditoId,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta,
        @Param("metodoPago") String metodoPago,
        @Param("sedeId") Long sedeId
    );
}