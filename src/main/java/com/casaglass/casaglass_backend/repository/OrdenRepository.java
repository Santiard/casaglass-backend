package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Orden;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByNumero(Long numero);

    List<Orden> findByClienteId(Long clienteId);

    /**
     * Encuentra órdenes de un cliente en un rango de fechas
     * Optimizado para mejorar rendimiento al filtrar en la base de datos
     */
    List<Orden> findByClienteIdAndFechaBetween(Long clienteId, LocalDate fechaDesde, LocalDate fechaHasta);

    List<Orden> findByVenta(boolean venta);     // true = ventas, false = cotizaciones

    List<Orden> findByCredito(boolean credito); // true = a crédito

    Optional<Orden> findByCreditoDetalleId(Long creditoDetalleId);

    List<Orden> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Orden> findBySedeId(Long sedeId);

    List<Orden> findByClienteIdAndSedeId(Long clienteId, Long sedeId);

    List<Orden> findBySedeIdAndVenta(Long sedeId, boolean venta);

    List<Orden> findBySedeIdAndCredito(Long sedeId, boolean credito);

    List<Orden> findBySedeIdAndFechaBetween(Long sedeId, LocalDate desde, LocalDate hasta);

    // 🆕 Métodos para filtrar por trabajador
    List<Orden> findByTrabajadorId(Long trabajadorId);

    List<Orden> findByTrabajadorIdAndVenta(Long trabajadorId, boolean venta);

    List<Orden> findByTrabajadorIdAndFechaBetween(Long trabajadorId, LocalDate desde, LocalDate hasta);

    List<Orden> findBySedeIdAndTrabajadorId(Long sedeId, Long trabajadorId);

    List<Orden> findAllById(Iterable<Long> ids);

    // Método para obtener el siguiente número de orden disponible (thread-safe)
    @Query("SELECT COALESCE(MAX(o.numero), 0) + 1 FROM Orden o")
    Long obtenerSiguienteNumero();
    
    @EntityGraph(attributePaths = {"cliente", "sede", "items", "items.producto"})
    @Query("SELECT o FROM Orden o")
    List<Orden> findAllWithFullRelations();

    /**
     * 🔍 OBTENER ORDEN POR ID CON TODAS LAS RELACIONES CARGADAS
     * Usa fetch joins para cargar todas las relaciones de una vez y evitar problemas de lazy loading
     * Especialmente útil para órdenes facturadas donde la sesión puede estar cerrada
     * 
     * Carga:
     * - Items de la orden
     * - Producto de cada item
     * - Cliente de la orden
     * - Sede de la orden
     * - Trabajador de la orden
     * - Factura asociada (si existe)
     */
    @Query("SELECT DISTINCT o FROM Orden o " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH o.cliente c " +
           "LEFT JOIN FETCH o.sede s " +
           "LEFT JOIN FETCH o.trabajador t " +
           "LEFT JOIN FETCH o.factura f " +
           "WHERE o.id = :id")
    Optional<Orden> findByIdWithAllRelations(@Param("id") Long id);

    /**
     * 💰 ÓRDENES A CONTADO DISPONIBLES PARA ENTREGA
     * - De la sede especificada
     * - En el período indicado
     * - Venta a contado (credito = false)
     * - No incluidas en otra entrega (incluidaEntrega = false)
     * - Estado ACTIVA
     */
    /**
     * 💰 ÓRDENES A CONTADO DISPONIBLES PARA ENTREGA
     * ⚠️ EXCLUYE al cliente especial (ID 499 - JAIRO JAVIER VELANDIA)
     */
    @Query("SELECT o FROM Orden o WHERE " +
           "o.cliente.id != 499 AND " + // ⚠️ EXCLUIR CLIENTE ESPECIAL
           "o.sede.id = :sedeId AND " +
           "o.fecha BETWEEN :fechaDesde AND :fechaHasta AND " +
           "o.credito = false AND " +
           "o.venta = true AND " +
           "o.incluidaEntrega = false AND " +
           "o.estado = 'ACTIVA'")
    List<Orden> findOrdenesContadoDisponiblesParaEntrega(
        @Param("sedeId") Long sedeId,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta
    );

    /**
     * 💰 ÓRDENES A CONTADO DISPONIBLES PARA ENTREGA (SIN FILTRO DE FECHA)
     * Trae todas las órdenes de la sede que aún no están incluidas en entrega.
     */
    @Query("SELECT o FROM Orden o WHERE " +
           "o.cliente.id != 499 AND " +
           "o.sede.id = :sedeId AND " +
           "o.credito = false AND " +
           "o.venta = true AND " +
           "o.incluidaEntrega = false AND " +
           "o.estado = 'ACTIVA'")
    List<Orden> findOrdenesContadoDisponiblesParaEntregaSinFecha(
        @Param("sedeId") Long sedeId
    );

    /**
     * 🏦 ÓRDENES A CRÉDITO CON ABONOS EN EL PERÍODO
     * - De la sede especificada
     * - Venta a crédito (credito = true)
     * - Que tengan abonos en el período especificado
     * - Estado ACTIVA
     * - No incluidas en otra entrega (incluidaEntrega = false)
     * - Crédito abierto (no cerrado)
     */
    @Query("SELECT DISTINCT o FROM Orden o " +
           "JOIN o.creditoDetalle c " +
           "JOIN c.abonos a WHERE " +
           "o.sede.id = :sedeId AND " +
           "o.credito = true AND " +
           "o.venta = true AND " +
           "o.estado = 'ACTIVA' AND " +
           "o.incluidaEntrega = false AND " +
           "c.estado = 'ABIERTO' AND " +
           "a.fecha BETWEEN :fechaDesde AND :fechaHasta")
    List<Orden> findOrdenesConAbonosEnPeriodo(
        @Param("sedeId") Long sedeId,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta
    );

    // 📊 MÉTODO PARA DASHBOARD - VENTAS DE HOY
    List<Orden> findBySedeIdAndFechaAndVentaTrue(Long sedeId, LocalDate fecha);

    /**
     * 🔍 BÚSQUEDA AVANZADA DE ÓRDENES CON MÚLTIPLES FILTROS
     * Usado para GET /api/ordenes/tabla con filtros opcionales
     * Todos los parámetros son opcionales (nullable)
     * El ordenamiento se maneja en el servicio
     */
    @Query("SELECT DISTINCT o FROM Orden o " +
           "LEFT JOIN o.factura f " +
           "LEFT JOIN o.creditoDetalle c " +
           "WHERE (:clienteId IS NULL OR o.cliente.id = :clienteId) AND " +
           "(:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
           "(:estado IS NULL OR o.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR o.fecha >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR o.fecha <= :fechaHasta) AND " +
           "(:venta IS NULL OR o.venta = :venta) AND " +
           "(:credito IS NULL OR o.credito = :credito) AND " +
           "(:estadoPago IS NULL OR " +
           "  (:estadoPago = 'PAGADO' AND ((o.venta = true AND o.credito = false) OR (o.venta = true AND o.credito = true AND COALESCE(c.saldoPendiente, 0) <= 0.01))) OR " +
           "  (:estadoPago = 'ABONADO' AND (o.venta = true AND o.credito = true AND COALESCE(c.saldoPendiente, 0) > 0.01 AND COALESCE(c.totalAbonado, 0) > 0.01)) OR " +
           "  (:estadoPago = 'NO PAGADO' AND ((o.venta = false) OR (o.venta = true AND o.credito = true AND (c.id IS NULL OR (COALESCE(c.saldoPendiente, 0) > 0.01 AND COALESCE(c.totalAbonado, 0) <= 0.01)))))" +
           ") AND " +
           "(:facturada IS NULL OR (:facturada = true AND f.id IS NOT NULL) OR (:facturada = false AND f.id IS NULL)) " +
           "ORDER BY o.fecha DESC, o.numero DESC")
    List<Orden> buscarConFiltros(
        @Param("clienteId") Long clienteId,
        @Param("sedeId") Long sedeId,
        @Param("estado") Orden.EstadoOrden estado,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta,
        @Param("venta") Boolean venta,
        @Param("credito") Boolean credito,
        @Param("estadoPago") String estadoPago,
        @Param("facturada") Boolean facturada
    );
}