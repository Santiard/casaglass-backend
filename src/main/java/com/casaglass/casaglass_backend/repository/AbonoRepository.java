package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Abono;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonoRepository extends JpaRepository<Abono, Long> {
    List<Abono> findByCreditoId(Long creditoId);

    List<Abono> findByClienteId(Long clienteId);

    List<Abono> findByOrdenId(Long ordenId);
}