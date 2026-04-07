package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.OrdenCortePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenCortePlanRepository extends JpaRepository<OrdenCortePlan, Long> {

    List<OrdenCortePlan> findByOrdenIdAndEstadoOrderByPlanOrdenAsc(Long ordenId, OrdenCortePlan.EstadoPlanCorte estado);

    void deleteByOrdenIdAndEstado(Long ordenId, OrdenCortePlan.EstadoPlanCorte estado);

    void deleteByOrdenId(Long ordenId);
}
