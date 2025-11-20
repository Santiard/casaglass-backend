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

    List<Orden> findByVenta(boolean venta);     // true = ventas, false = cotizaciones

    List<Orden> findByCredito(boolean credito); // true = a crédito

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
     * 💰 ÓRDENES A CONTADO DISPONIBLES PARA ENTREGA
     * - De la sede especificada
     * - En el período indicado
     * - Venta a contado (credito = false)
     * - No incluidas en otra entrega (incluidaEntrega = false)
     * - Estado ACTIVA
     */
    @Query("SELECT o FROM Orden o WHERE " +
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
}