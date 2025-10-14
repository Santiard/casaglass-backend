package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Corte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CorteRepository extends JpaRepository<Corte, Long> {

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

    @Query("SELECT c FROM Corte c WHERE c.precio BETWEEN :precioMin AND :precioMax")
    List<Corte> findByPrecioRange(@Param("precioMin") Double precioMin, 
                                  @Param("precioMax") Double precioMax);

    List<Corte> findByLargoCmGreaterThanEqual(Double largoMinimo);

    List<Corte> findByPrecioLessThanEqual(Double precioMaximo);

    @Query("SELECT c FROM Corte c WHERE c.observacion IS NOT NULL AND c.observacion != ''")
    List<Corte> findCortesWithObservaciones();

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
}