package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {

    Optional<Trabajador> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    List<Trabajador> findByRol(Rol rol);

    List<Trabajador> findByNombreContainingIgnoreCase(String nombre);

    List<Trabajador> findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCase(String nombre, String correo);
}
