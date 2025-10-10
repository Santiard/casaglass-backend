package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.GastoSede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GastoSedeRepository extends JpaRepository<GastoSede, Long> {

    List<GastoSede> findBySedeId(Long sedeId);

    List<GastoSede> findByEntregaId(Long entregaId);

    List<GastoSede> findByTipo(GastoSede.TipoGasto tipo);

    List<GastoSede> findBySedeIdAndTipo(Long sedeId, GastoSede.TipoGasto tipo);

    List<GastoSede> findByFechaGastoBetween(LocalDate desde, LocalDate hasta);

    List<GastoSede> findBySedeIdAndFechaGastoBetween(Long sedeId, LocalDate desde, LocalDate hasta);

    List<GastoSede> findByAprobado(Boolean aprobado);

    List<GastoSede> findBySedeIdAndAprobado(Long sedeId, Boolean aprobado);

    @Query("SELECT g FROM GastoSede g WHERE g.entrega IS NULL AND g.sede.id = :sedeId")
    List<GastoSede> findGastosSinEntregaBySede(@Param("sedeId") Long sedeId);

    @Query("SELECT SUM(g.monto) FROM GastoSede g WHERE g.sede.id = :sedeId AND g.fechaGasto BETWEEN :desde AND :hasta")
    Double getTotalGastosBySedeAndPeriodo(@Param("sedeId") Long sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT SUM(g.monto) FROM GastoSede g WHERE g.sede.id = :sedeId AND g.tipo = :tipo AND g.fechaGasto BETWEEN :desde AND :hasta")
    Double getTotalGastosBySedeAndTipoAndPeriodo(@Param("sedeId") Long sedeId, @Param("tipo") GastoSede.TipoGasto tipo, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT g FROM GastoSede g WHERE g.concepto LIKE %:concepto%")
    List<GastoSede> findByConceptoContaining(@Param("concepto") String concepto);

    @Query("SELECT g.concepto, SUM(g.monto) FROM GastoSede g WHERE g.sede.id = :sedeId AND g.fechaGasto BETWEEN :desde AND :hasta GROUP BY g.concepto")
    List<Object[]> getResumenGastosByConcepto(@Param("sedeId") Long sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}