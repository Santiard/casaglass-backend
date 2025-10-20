package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Credito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditoRepository extends JpaRepository<Credito, Long> {
    
    // Búsqueda por cliente (para el nuevo modelo permite múltiples créditos por cliente)
    List<Credito> findByClienteId(Long clienteId);
    
    // Búsqueda por orden (en el nuevo modelo cada crédito tiene una orden específica)
    Optional<Credito> findByOrdenId(Long ordenId);
    
    // Búsqueda por estado
    List<Credito> findByEstado(Credito.EstadoCredito estado);
    
    // Métodos adicionales para consultas específicas
    List<Credito> findByClienteIdAndEstado(Long clienteId, Credito.EstadoCredito estado);
    
    // Para compatibilidad con código anterior (método único)
    Optional<Credito> findFirstByClienteId(Long clienteId);
}