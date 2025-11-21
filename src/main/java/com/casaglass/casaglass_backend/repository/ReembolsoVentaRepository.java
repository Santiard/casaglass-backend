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
}

