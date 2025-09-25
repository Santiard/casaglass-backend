package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.InventarioCorte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventarioCorteRepository extends JpaRepository<InventarioCorte, Long> {

    List<InventarioCorte> findByCorteId(Long corteId);

    List<InventarioCorte> findBySedeId(Long sedeId);

    Optional<InventarioCorte> findByCorteIdAndSedeId(Long corteId, Long sedeId);

    @Query("SELECT ic FROM InventarioCorte ic WHERE ic.cantidad > 0")
    List<InventarioCorte> findAllWithStock();

    @Query("SELECT ic FROM InventarioCorte ic WHERE ic.cantidad = 0")
    List<InventarioCorte> findAllWithoutStock();

    @Query("SELECT ic FROM InventarioCorte ic WHERE ic.sede.id = :sedeId AND ic.cantidad > 0")
    List<InventarioCorte> findBySedeIdWithStock(@Param("sedeId") Long sedeId);

    @Query("SELECT ic FROM InventarioCorte ic WHERE ic.corte.id = :corteId AND ic.cantidad > 0")
    List<InventarioCorte> findByCorteIdWithStock(@Param("corteId") Long corteId);

    @Query("SELECT ic FROM InventarioCorte ic WHERE ic.cantidad BETWEEN :cantidadMin AND :cantidadMax")
    List<InventarioCorte> findByCantidadRange(@Param("cantidadMin") Integer cantidadMin, 
                                              @Param("cantidadMax") Integer cantidadMax);

    @Query("SELECT SUM(ic.cantidad) FROM InventarioCorte ic WHERE ic.corte.id = :corteId")
    Integer getTotalStockByCorteId(@Param("corteId") Long corteId);

    @Query("SELECT SUM(ic.cantidad) FROM InventarioCorte ic WHERE ic.sede.id = :sedeId")
    Integer getTotalStockBySedeId(@Param("sedeId") Long sedeId);

    @Query("SELECT DISTINCT ic.sede.id FROM InventarioCorte ic WHERE ic.corte.id = :corteId AND ic.cantidad > 0")
    List<Long> findSedesWithStockByCorteId(@Param("corteId") Long corteId);
}