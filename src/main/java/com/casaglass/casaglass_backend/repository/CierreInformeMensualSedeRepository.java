package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.CierreInformeMensualSede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CierreInformeMensualSedeRepository extends JpaRepository<CierreInformeMensualSede, Long> {

    Optional<CierreInformeMensualSede> findBySedeIdAndAnioAndMes(Long sedeId, Integer anio, Integer mes);

    List<CierreInformeMensualSede> findBySedeIdAndAnioOrderByMesAsc(Long sedeId, Integer anio);
}
