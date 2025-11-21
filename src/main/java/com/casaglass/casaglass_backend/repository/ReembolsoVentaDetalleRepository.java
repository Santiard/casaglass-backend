package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.ReembolsoVentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReembolsoVentaDetalleRepository extends JpaRepository<ReembolsoVentaDetalle, Long> {
    
    // Obtener todos los detalles de un reembolso
    List<ReembolsoVentaDetalle> findByReembolsoVentaId(Long reembolsoVentaId);
    
    // Obtener todos los reembolsos de un item de orden específico
    @Query("SELECT r FROM ReembolsoVentaDetalle r WHERE r.ordenItemOriginal.id = :ordenItemId")
    List<ReembolsoVentaDetalle> findByOrdenItemOriginalId(@Param("ordenItemId") Long ordenItemId);
}

