package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Corte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // 🆕 Nuevos métodos para filtrar por tipo
    List<Producto> findByTipo(com.casaglass.casaglass_backend.model.TipoProducto tipo);

    List<Producto> findByCategoria_IdAndTipo(Long categoriaId, com.casaglass.casaglass_backend.model.TipoProducto tipo);

    // 🆕 Nuevos métodos para filtrar por color
    List<Producto> findByColor(com.casaglass.casaglass_backend.model.ColorProducto color);

    List<Producto> findByCategoria_IdAndColor(Long categoriaId, com.casaglass.casaglass_backend.model.ColorProducto color);

    // 🆕 Método para buscar por lista de IDs
    List<Producto> findByIdIn(List<Long> ids);

    // 🔧 NUEVO: Obtener solo productos base (excluir cortes y productos vidrio)
    @Query("SELECT p FROM Producto p WHERE TYPE(p) = Producto")
    List<Producto> findProductosBase();

    // 🔧 ALTERNATIVO: Excluir cortes usando discriminador
    @Query("SELECT p FROM Producto p WHERE p NOT IN (SELECT c FROM Corte c)")
    List<Producto> findProductosSinCortes();

    // 🔧 NUEVO: Buscar por categoría excluyendo cortes
    @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId AND p NOT IN (SELECT c FROM Corte c)")
    List<Producto> findByCategoria_IdSinCortes(@Param("categoriaId") Long categoriaId);

    // 🔧 NUEVO: Búsqueda por nombre/código excluyendo cortes
    @Query("SELECT p FROM Producto p WHERE (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :codigo, '%'))) AND p NOT IN (SELECT c FROM Corte c)")
    List<Producto> findByNombreOrCodigoSinCortes(@Param("nombre") String nombre, @Param("codigo") String codigo);

    // 🔧 NUEVO: Buscar por tipo excluyendo cortes
    @Query("SELECT p FROM Producto p WHERE p.tipo = :tipo AND p NOT IN (SELECT c FROM Corte c)")
    List<Producto> findByTipoSinCortes(@Param("tipo") com.casaglass.casaglass_backend.model.TipoProducto tipo);

    // 🔧 NUEVO: Buscar por color excluyendo cortes
    @Query("SELECT p FROM Producto p WHERE p.color = :color AND p NOT IN (SELECT c FROM Corte c)")
    List<Producto> findByColorSinCortes(@Param("color") com.casaglass.casaglass_backend.model.ColorProducto color);
}
