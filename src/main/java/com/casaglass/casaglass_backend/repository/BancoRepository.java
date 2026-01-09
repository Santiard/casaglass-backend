package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BancoRepository extends JpaRepository<Banco, Long> {
    
    /**
     * Buscar banco por nombre exacto
     */
    Optional<Banco> findByNombre(String nombre);
    
    /**
     * Verificar si existe un banco con ese nombre (case insensitive)
     */
    boolean existsByNombreIgnoreCase(String nombre);
}
