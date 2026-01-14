package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.EntregaClienteEspecial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EntregaClienteEspecialRepository extends JpaRepository<EntregaClienteEspecial, Long> {

    @Query("SELECT e FROM EntregaClienteEspecial e WHERE (:desde IS NULL OR e.fechaRegistro >= :desde) " +
           "AND (:hasta IS NULL OR e.fechaRegistro <= :hasta) ORDER BY e.fechaRegistro DESC")
    List<EntregaClienteEspecial> buscarPorRango(@Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta);

    @EntityGraph(attributePaths = {"detalles", "detalles.credito", "detalles.orden"})
    Optional<EntregaClienteEspecial> findWithDetallesById(Long id);
}
