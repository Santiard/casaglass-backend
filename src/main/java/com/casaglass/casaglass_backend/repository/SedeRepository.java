package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SedeRepository extends JpaRepository<Sede, Long> {

    Optional<Sede> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    // Búsquedas libres (contiene, case-insensitive)
    List<Sede> findByNombreContainingIgnoreCase(String nombre);

    List<Sede> findByCiudadContainingIgnoreCase(String ciudad);

    List<Sede> findByNombreContainingIgnoreCaseOrCiudadContainingIgnoreCase(String nombre, String ciudad);
}
