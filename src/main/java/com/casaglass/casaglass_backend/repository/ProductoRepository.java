package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.model.ColorProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Buscar por código (puede haber múltiples productos con el mismo código)
    // Retorna el primero que encuentra
    Optional<Producto> findByCodigo(String codigo);
    
    // Buscar todos los productos con el mismo código
    List<Producto> findAllByCodigo(String codigo);

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

    /**
     * 🔍 BÚSQUEDA AVANZADA DE PRODUCTOS CON MÚLTIPLES FILTROS
     * Todos los parámetros son opcionales (nullable)
     * ✅ Incluye: Producto base y ProductoVidrio
     * ❌ Excluye: Corte
     */
    @Query("SELECT DISTINCT p FROM Producto p " +
           "LEFT JOIN FETCH p.categoria c " +
           "WHERE TYPE(p) != Corte AND " +
           "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) AND " +
           "(:categoriaNombre IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :categoriaNombre, '%'))) AND " +
           "(:tipo IS NULL OR p.tipo = :tipo) AND " +
           "(:color IS NULL OR p.color = :color) AND " +
           "(:codigo IS NULL OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :codigo, '%'))) AND " +
           "(:nombre IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
           "ORDER BY p.codigo ASC, p.nombre ASC")
    List<Producto> buscarConFiltros(
        @Param("categoriaId") Long categoriaId,
        @Param("categoriaNombre") String categoriaNombre,
        @Param("tipo") com.casaglass.casaglass_backend.model.TipoProducto tipo,
        @Param("color") com.casaglass.casaglass_backend.model.ColorProducto color,
        @Param("codigo") String codigo,
        @Param("nombre") String nombre
    );

    /**
     * 📍 Obtener la máxima posición numérica de todos los productos
     * Retorna null si no hay productos con posición
     * Usa consulta nativa para mejor compatibilidad con MariaDB
     */
    @Query(value = "SELECT MAX(CAST(posicion AS UNSIGNED)) FROM productos WHERE posicion IS NOT NULL AND posicion != ''", nativeQuery = true)
    Long obtenerMaximaPosicion();

    /**
     * 📍 Obtener productos con posición (excluyendo Cortes)
     * Útil para correr posiciones al insertar un nuevo producto
     * Usa JPQL para manejar correctamente la herencia JOINED
     * El filtrado por posición numérica se hace en Java para evitar problemas con CAST en JPQL
     */
    @Query("SELECT p FROM Producto p WHERE TYPE(p) != Corte AND p.posicion IS NOT NULL AND p.posicion != ''")
    List<Producto> encontrarProductosConPosicion();

    @Query(
        value = "SELECT p.id AS id, p.codigo AS codigo, p.nombre AS nombre, " +
                "c.id AS categoriaId, c.nombre AS categoriaNombre, p.color AS color, " +
                "COALESCE((SELECT i.cantidad FROM Inventario i WHERE i.producto.id = p.id AND i.sede.id = :sedeOrigenId), 0.0) AS cantidadSedeOrigen, " +
                "COALESCE((SELECT SUM(it.cantidad) FROM Inventario it WHERE it.producto.id = p.id), 0.0) AS cantidadTotal, " +
                "p.precio1 AS precio1, p.precio2 AS precio2, p.precio3 AS precio3 " +
                "FROM Producto p " +
                "LEFT JOIN p.categoria c " +
                "WHERE TYPE(p) != Corte " +
                "AND (:q IS NULL OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                "AND (:categoriaId IS NULL OR c.id = :categoriaId) " +
                "AND (:color IS NULL OR p.color = :color)",
        countQuery = "SELECT COUNT(p) " +
                "FROM Producto p " +
                "LEFT JOIN p.categoria c " +
                "WHERE TYPE(p) != Corte " +
                "AND (:q IS NULL OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                "AND (:categoriaId IS NULL OR c.id = :categoriaId) " +
                "AND (:color IS NULL OR p.color = :color)"
    )
    Page<CatalogoProductoTrasladoProjection> buscarCatalogoParaTraslado(
        @Param("sedeOrigenId") Long sedeOrigenId,
        @Param("q") String q,
        @Param("categoriaId") Long categoriaId,
        @Param("color") ColorProducto color,
        Pageable pageable
    );
}
