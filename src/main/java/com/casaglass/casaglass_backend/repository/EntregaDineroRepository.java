package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.EntregaDinero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EntregaDineroRepository extends JpaRepository<EntregaDinero, Long> {

    List<EntregaDinero> findBySedeId(Long sedeId);

    List<EntregaDinero> findByEmpleadoId(Long empleadoId);

    List<EntregaDinero> findByEstado(EntregaDinero.EstadoEntrega estado);

    List<EntregaDinero> findBySedeIdAndEstado(Long sedeId, EntregaDinero.EstadoEntrega estado);

    List<EntregaDinero> findByFechaEntregaBetween(LocalDate desde, LocalDate hasta);

    List<EntregaDinero> findBySedeIdAndFechaEntregaBetween(Long sedeId, LocalDate desde, LocalDate hasta);

    @Query("SELECT e FROM EntregaDinero e WHERE e.diferencia <> 0")
    List<EntregaDinero> findEntregasWithDifferences();

    @Query("SELECT e FROM EntregaDinero e WHERE e.sede.id = :sedeId AND e.diferencia <> 0")
    List<EntregaDinero> findEntregasConDiferenciasBySede(@Param("sedeId") Long sedeId);

    @Query("SELECT SUM(e.montoEntregado) FROM EntregaDinero e WHERE e.sede.id = :sedeId AND e.estado = :estado")
    Double getTotalEntregadoBySede(@Param("sedeId") Long sedeId, @Param("estado") EntregaDinero.EstadoEntrega estado);

    @Query("SELECT SUM(e.montoEntregado) FROM EntregaDinero e WHERE e.sede.id = :sedeId AND e.fechaEntrega BETWEEN :desde AND :hasta")
    Double getTotalEntregadoBySedeAndPeriodo(@Param("sedeId") Long sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT SUM(e.montoGastos) FROM EntregaDinero e WHERE e.sede.id = :sedeId AND e.fechaEntrega BETWEEN :desde AND :hasta")
    Double getTotalGastosBySedeAndPeriodo(@Param("sedeId") Long sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT e.empleado.nombre, COUNT(e), SUM(e.montoEntregado), SUM(e.montoGastos) " +
           "FROM EntregaDinero e WHERE e.sede.id = :sedeId AND e.fechaEntrega BETWEEN :desde AND :hasta " +
           "GROUP BY e.empleado.id, e.empleado.nombre")
    List<Object[]> getResumenByEmpleado(@Param("sedeId") Long sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT e FROM EntregaDinero e WHERE e.sede.id = :sedeId ORDER BY e.fechaEntrega DESC")
    List<EntregaDinero> findUltimasEntregasBySede(@Param("sedeId") Long sedeId);

    // Buscar la última entrega de una sede
    EntregaDinero findFirstBySedeIdOrderByFechaEntregaDesc(Long sedeId);
}