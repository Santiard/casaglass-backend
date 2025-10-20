package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    List<Inventario> findByProductoId(Long productoId);

    List<Inventario> findBySedeId(Long sedeId);

    Optional<Inventario> findByProductoIdAndSedeId(Long productoId, Long sedeId);

    // Nuevo: buscar inventarios para una lista de productos
    List<Inventario> findByProductoIdIn(List<Long> productoIds);
    
    // 📊 MÉTODO PARA DASHBOARD - STOCK BAJO POR SEDE CON FETCH JOINS
    @Query("SELECT DISTINCT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE i.sede.id = :sedeId AND i.cantidad <= :cantidad " +
           "ORDER BY i.cantidad ASC")
    List<Inventario> findBySedeIdAndCantidadLessThanEqual(@Param("sedeId") Long sedeId, @Param("cantidad") Integer cantidad);
}