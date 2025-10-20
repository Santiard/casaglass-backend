package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.EntregaDinero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntregaDineroRepository extends JpaRepository<EntregaDinero, Long> {

    @Override
    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado")
    List<EntregaDinero> findAll();

    @Override
    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.id = :id")
    Optional<EntregaDinero> findById(@Param("id") Long id);

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.sede.id = :sedeId")
    List<EntregaDinero> findBySedeId(@Param("sedeId") Long sedeId);

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.empleado.id = :empleadoId")
    List<EntregaDinero> findByEmpleadoId(@Param("empleadoId") Long empleadoId);

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.estado = :estado")
    List<EntregaDinero> findByEstado(@Param("estado") EntregaDinero.EstadoEntrega estado);

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.sede.id = :sedeId AND e.estado = :estado")
    List<EntregaDinero> findBySedeIdAndEstado(@Param("sedeId") Long sedeId, @Param("estado") EntregaDinero.EstadoEntrega estado);

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.fechaEntrega BETWEEN :desde AND :hasta")
    List<EntregaDinero> findByFechaEntregaBetween(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.sede.id = :sedeId AND e.fechaEntrega BETWEEN :desde AND :hasta")
    List<EntregaDinero> findBySedeIdAndFechaEntregaBetween(@Param("sedeId") Long sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.diferencia <> 0")
    List<EntregaDinero> findEntregasWithDifferences();

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.sede.id = :sedeId AND e.diferencia <> 0")
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

    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.sede.id = :sedeId ORDER BY e.fechaEntrega DESC")
    List<EntregaDinero> findUltimasEntregasBySede(@Param("sedeId") Long sedeId);

    // Buscar la última entrega de una sede
    @Query("SELECT DISTINCT e FROM EntregaDinero e " +
           "LEFT JOIN FETCH e.sede " +
           "LEFT JOIN FETCH e.empleado " +
           "WHERE e.sede.id = :sedeId ORDER BY e.fechaEntrega DESC")
    List<EntregaDinero> findFirstBySedeIdOrderByFechaEntregaDescWithFetch(@Param("sedeId") Long sedeId);
    
    // Mantener el método original para compatibilidad
    EntregaDinero findFirstBySedeIdOrderByFechaEntregaDesc(Long sedeId);
}