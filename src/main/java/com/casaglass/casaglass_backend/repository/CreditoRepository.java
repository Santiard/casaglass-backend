package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Credito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditoRepository extends JpaRepository<Credito, Long> {
    Optional<Credito> findByClienteId(Long clienteId);
    List<Credito> findAllByClienteId(Long clienteId); // por si permites >1 crédito por cliente
}