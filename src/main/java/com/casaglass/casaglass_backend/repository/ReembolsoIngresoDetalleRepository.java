package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.ReembolsoIngresoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReembolsoIngresoDetalleRepository extends JpaRepository<ReembolsoIngresoDetalle, Long> {
    
    // Obtener todos los detalles de un reembolso
    List<ReembolsoIngresoDetalle> findByReembolsoIngresoId(Long reembolsoIngresoId);
    
    // Obtener todos los reembolsos de un detalle de ingreso específico
    @Query("SELECT r FROM ReembolsoIngresoDetalle r WHERE r.ingresoDetalleOriginal.id = :ingresoDetalleId")
    List<ReembolsoIngresoDetalle> findByIngresoDetalleOriginalId(@Param("ingresoDetalleId") Long ingresoDetalleId);
}

