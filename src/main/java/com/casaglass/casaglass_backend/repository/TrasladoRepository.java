package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Traslado;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

    @EntityGraph(attributePaths = {
        "sedeOrigen",
        "sedeDestino",
        "trabajadorConfirmacion",
        "detalles",
        "detalles.producto"
    })
    @Query("select t from Traslado t")
    List<Traslado> findAllEager();

    List<Traslado> findBySedeOrigenId(Long sedeOrigenId);

    List<Traslado> findBySedeDestinoId(Long sedeDestinoId);

    List<Traslado> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Traslado> findBySedeOrigenIdAndSedeDestinoId(Long sedeOrigenId, Long sedeDestinoId);
}