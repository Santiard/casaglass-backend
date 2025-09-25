package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Corte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CorteRepository extends JpaRepository<Corte, Long> {

    Optional<Corte> findByCodigo(String codigo);

    List<Corte> findByCategoriaIgnoreCase(String categoria);

    List<Corte> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(
            String nombre, String codigo
    );

    @Query("SELECT c FROM Corte c WHERE c.largoCm BETWEEN :largoMin AND :largoMax")
    List<Corte> findByLargoRange(@Param("largoMin") BigDecimal largoMin, 
                                 @Param("largoMax") BigDecimal largoMax);

    @Query("SELECT c FROM Corte c WHERE c.precio BETWEEN :precioMin AND :precioMax")
    List<Corte> findByPrecioRange(@Param("precioMin") BigDecimal precioMin, 
                                  @Param("precioMax") BigDecimal precioMax);

    List<Corte> findByLargoCmGreaterThanEqual(BigDecimal largoMinimo);

    List<Corte> findByPrecioLessThanEqual(BigDecimal precioMaximo);

    @Query("SELECT c FROM Corte c WHERE c.observacion IS NOT NULL AND c.observacion != ''")
    List<Corte> findCortesWithObservaciones();
}