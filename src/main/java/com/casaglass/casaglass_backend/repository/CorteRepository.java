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
    List<Corte> findByLargoRange(@Param("largoMin") Double largoMin, 
                                 @Param("largoMax") Double largoMax);

    @Query("SELECT c FROM Corte c WHERE c.precio BETWEEN :precioMin AND :precioMax")
    List<Corte> findByPrecioRange(@Param("precioMin") Double precioMin, 
                                  @Param("precioMax") Double precioMax);

    List<Corte> findByLargoCmGreaterThanEqual(Double largoMinimo);

    List<Corte> findByPrecioLessThanEqual(Double precioMaximo);

    @Query("SELECT c FROM Corte c WHERE c.observacion IS NOT NULL AND c.observacion != ''")
    List<Corte> findCortesWithObservaciones();
}