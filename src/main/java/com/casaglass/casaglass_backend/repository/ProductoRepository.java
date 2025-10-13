package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Buscar por código único
    Optional<Producto> findByCodigo(String codigo);

    // Buscar productos cuyo nombre o código contengan un texto (búsqueda general)
    List<Producto> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(String nombre, String codigo);

    // 🔁 Nuevo: buscar por ID de categoría (FK)
    List<Producto> findByCategoria_Id(Long categoriaId);

    // 🔁 Nuevo: buscar por nombre de categoría (relación)
    List<Producto> findByCategoria_NombreIgnoreCase(String nombre);

    // 🔁 Mantener compatibilidad con el método listarCategoriasTexto() del servicio
    @Query("SELECT DISTINCT c.nombre FROM Producto p JOIN p.categoria c WHERE c.nombre IS NOT NULL ORDER BY c.nombre")
    List<String> findDistinctCategorias();
}
