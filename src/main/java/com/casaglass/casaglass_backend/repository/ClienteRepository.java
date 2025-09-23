package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNit(String nit);
    Optional<Cliente> findByCorreo(String correo);
}