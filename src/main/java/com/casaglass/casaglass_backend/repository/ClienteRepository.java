package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNit(String nit);
    Optional<Cliente> findByCorreo(String correo);

    /**
     * 🔍 BÚSQUEDA AVANZADA DE CLIENTES CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     * Nota: activo no está implementado (el modelo no tiene campo activo)
     * Nota: conCredito requiere verificación adicional en el servicio
     */
    @Query("SELECT DISTINCT c FROM Cliente c " +
           "WHERE (:nombre IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:nit IS NULL OR LOWER(c.nit) LIKE LOWER(CONCAT('%', :nit, '%'))) AND " +
           "(:correo IS NULL OR LOWER(c.correo) LIKE LOWER(CONCAT('%', :correo, '%'))) AND " +
           "(:ciudad IS NULL OR LOWER(c.ciudad) LIKE LOWER(CONCAT('%', :ciudad, '%'))) " +
           "ORDER BY c.nombre ASC")
    List<Cliente> buscarConFiltros(
        @Param("nombre") String nombre,
        @Param("nit") String nit,
        @Param("correo") String correo,
        @Param("ciudad") String ciudad
    );
}