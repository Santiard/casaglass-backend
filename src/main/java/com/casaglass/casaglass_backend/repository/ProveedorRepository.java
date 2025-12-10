package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByNit(String nit);

    /**
     * 🔍 BÚSQUEDA AVANZADA DE PROVEEDORES CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     * Nota: activo y correo no están implementados (el modelo no tiene esos campos)
     */
    @Query("SELECT DISTINCT p FROM Proveedor p " +
           "WHERE (:nombre IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:nit IS NULL OR LOWER(p.nit) LIKE LOWER(CONCAT('%', :nit, '%'))) AND " +
           "(:ciudad IS NULL OR LOWER(p.ciudad) LIKE LOWER(CONCAT('%', :ciudad, '%'))) " +
           "ORDER BY p.nombre ASC")
    List<Proveedor> buscarConFiltros(
        @Param("nombre") String nombre,
        @Param("nit") String nit,
        @Param("ciudad") String ciudad
    );
}