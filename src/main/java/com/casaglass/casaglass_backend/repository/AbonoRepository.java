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

    List<Abono> findByOrdenId(Long ordenId);

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
}