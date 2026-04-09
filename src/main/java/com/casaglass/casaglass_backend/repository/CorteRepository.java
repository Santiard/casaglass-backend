package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Corte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CorteRepository extends JpaRepository<Corte, Long> {

        // Buscar corte por código, color, categoría y largo
        List<Corte> findByCodigoAndColorAndCategoria_IdAndLargoCm(String codigo, com.casaglass.casaglass_backend.model.ColorProducto color, Long categoriaId, Double largoCm);

    Optional<Corte> findByCodigo(String codigo);

    // 🔁 Actualizado para usar Categoria como entidad
    List<Corte> findByCategoria_Id(Long categoriaId);
    List<Corte> findByCategoria_NombreIgnoreCase(String categoriaNombre);

    List<Corte> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(
            String nombre, String codigo
    );

    @Query("SELECT c FROM Corte c WHERE c.largoCm BETWEEN :largoMin AND :largoMax")
    List<Corte> findByLargoRange(@Param("largoMin") Double largoMin, 
                                 @Param("largoMax") Double largoMax);

    @Query("SELECT c FROM Corte c WHERE c.precio1 BETWEEN :precioMin AND :precioMax")
    List<Corte> findByPrecioRange(@Param("precioMin") Double precioMin, 
                                  @Param("precioMax") Double precioMax);

    List<Corte> findByLargoCmGreaterThanEqual(Double largoMinimo);

    List<Corte> findByPrecio1LessThanEqual(Double precioMaximo);

    // Nuevo: buscar por rango de largo usando método Spring Data JPA
    List<Corte> findByLargoCmBetween(Double largoMin, Double largoMax);

    // 🆕 Nuevos métodos para filtrar por tipo
    List<Corte> findByTipo(com.casaglass.casaglass_backend.model.TipoProducto tipo);

    List<Corte> findByCategoria_IdAndTipo(Long categoriaId, com.casaglass.casaglass_backend.model.TipoProducto tipo);

    // 🆕 Nuevos métodos para filtrar por color
    List<Corte> findByColor(com.casaglass.casaglass_backend.model.ColorProducto color);

    List<Corte> findByCategoria_IdAndColor(Long categoriaId, com.casaglass.casaglass_backend.model.ColorProducto color);

    // 🆕 Método para buscar por lista de IDs
    List<Corte> findByIdIn(List<Long> ids);

    // 🆕 Buscar corte existente por código base (sin sufijo), largo exacto, categoría y color
    // ✅ El código siempre es el del producto base (ej: "392"), no incluye la medida
    @Query("SELECT c FROM Corte c WHERE c.codigo = :codigo AND c.largoCm = :largo AND c.categoria.id = :categoriaId AND c.color = :color")
    Optional<Corte> findExistingByCodigoAndSpecs(@Param("codigo") String codigo,
                                                @Param("largo") Double largo,
                                                @Param("categoriaId") Long categoriaId,
                                                @Param("color") com.casaglass.casaglass_backend.model.ColorProducto color);

    @Query("SELECT c FROM Corte c JOIN InventarioCorte ic ON ic.corte.id = c.id " +
            "WHERE c.codigo = :codigo AND c.largoCm = :largo AND c.categoria.id = :categoriaId AND c.color = :color " +
            "AND ic.sede.id = :sedeId AND ic.cantidad > 0")
    Optional<Corte> findExistingByCodigoAndSpecsAndSedeWithStock(@Param("codigo") String codigo,
                                                                           @Param("largo") Double largo,
                                                                           @Param("categoriaId") Long categoriaId,
                                                                           @Param("color") com.casaglass.casaglass_backend.model.ColorProducto color,
                                                                           @Param("sedeId") Long sedeId);
}