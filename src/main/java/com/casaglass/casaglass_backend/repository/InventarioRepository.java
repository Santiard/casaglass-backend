package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    // 🔧 MÉTODOS CON FETCH JOINS para evitar LazyInitializationException
    @Query("SELECT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE p.id = :productoId")
    List<Inventario> findByProductoId(@Param("productoId") Long productoId);

    @Query("SELECT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE i.sede.id = :sedeId")
    List<Inventario> findBySedeId(@Param("sedeId") Long sedeId);

    /**
     * 🔒 BUSCAR INVENTARIO CON LOCK OPTIMISTA
     * 
     * Usa el campo @Version en la entidad Inventario para control de concurrencia
     * No bloquea registros → operaciones concurrentes sin esperas
     * Si hay conflicto real (muy raro) → OptimisticLockException
     */
    @Query("SELECT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE p.id = :productoId AND i.sede.id = :sedeId")
    Optional<Inventario> findByProductoIdAndSedeId(@Param("productoId") Long productoId, @Param("sedeId") Long sedeId);

    /**
     * @deprecated Usar findByProductoIdAndSedeId() - ahora usa lock optimista
     * Mantenido temporalmente para compatibilidad, redirige al método principal
     */
    @Deprecated
    default Optional<Inventario> findByProductoIdAndSedeIdWithLock(Long productoId, Long sedeId) {
        return findByProductoIdAndSedeId(productoId, sedeId);
    }

    // Nuevo: buscar inventarios para una lista de productos con FETCH
    @Query("SELECT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE p.id IN :productoIds")
    List<Inventario> findByProductoIdIn(@Param("productoIds") List<Long> productoIds);

    // 🔧 MÉTODO PARA OBTENER POR ID CON FETCH JOINS
    @Query("SELECT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE i.id = :id")
    Optional<Inventario> findByIdWithDetails(@Param("id") Long id);

    // 🔧 MÉTODO PARA LISTAR TODOS CON FETCH JOINS
    @Query("SELECT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "ORDER BY p.nombre, i.sede.nombre")
    List<Inventario> findAllWithDetails();
    
    // 📊 MÉTODO PARA DASHBOARD - STOCK BAJO POR SEDE CON FETCH JOINS
    @Query("SELECT DISTINCT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE i.sede.id = :sedeId AND i.cantidad <= :cantidad " +
           "ORDER BY i.cantidad ASC")
    List<Inventario> findBySedeIdAndCantidadLessThanEqual(@Param("sedeId") Long sedeId, @Param("cantidad") Integer cantidad);

    // 📊 MÉTODO PARA DASHBOARD - STOCK BAJO EN TODAS LAS SEDES
    @Query("SELECT DISTINCT i FROM Inventario i " +
           "LEFT JOIN FETCH i.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "LEFT JOIN FETCH i.sede " +
           "WHERE i.cantidad <= :cantidad " +
           "ORDER BY i.cantidad ASC")
    List<Inventario> findAllByCantidadLessThanEqual(@Param("cantidad") Integer cantidad);
}