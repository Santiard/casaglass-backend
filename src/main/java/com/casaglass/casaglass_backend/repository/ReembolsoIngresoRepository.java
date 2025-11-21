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
}

