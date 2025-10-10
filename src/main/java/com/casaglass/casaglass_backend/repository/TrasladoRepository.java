package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Traslado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TrasladoRepository extends JpaRepository<Traslado, Long> {

    List<Traslado> findBySedeOrigenId(Long sedeOrigenId);

    List<Traslado> findBySedeDestinoId(Long sedeDestinoId);

    List<Traslado> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<Traslado> findBySedeOrigenIdAndSedeDestinoId(Long sedeOrigenId, Long sedeDestinoId);
}