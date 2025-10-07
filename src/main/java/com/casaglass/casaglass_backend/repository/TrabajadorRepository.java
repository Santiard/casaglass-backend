package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.model.Rol;
import com.casaglass.casaglass_backend.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {

    Optional<Trabajador> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    List<Trabajador> findByRol(Rol rol);

    List<Trabajador> findBySede(Sede sede);

    List<Trabajador> findBySedeId(Long sedeId);

    List<Trabajador> findByRolAndSede(Rol rol, Sede sede);

    List<Trabajador> findByRolAndSedeId(Rol rol, Long sedeId);

    List<Trabajador> findByNombreContainingIgnoreCase(String nombre);

    List<Trabajador> findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrUsernameContainingIgnoreCase(String nombre, String correo, String username);

    Optional<Trabajador> findByUsernameAndPassword(String username, String password);

    Optional<Trabajador> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    
}
