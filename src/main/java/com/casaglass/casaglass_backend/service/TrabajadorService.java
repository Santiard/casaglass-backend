package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Rol;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrabajadorService {

    private final TrabajadorRepository repo;
    private final SedeRepository sedeRepository;

    public TrabajadorService(TrabajadorRepository repo, SedeRepository sedeRepository) {
        this.repo = repo;
        this.sedeRepository = sedeRepository;
    }

    public List<Trabajador> listar() {
        return repo.findAll();
    }

    public Optional<Trabajador> obtenerPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<Trabajador> obtenerPorCorreo(String correo) {
        return repo.findByCorreoIgnoreCase(correo);
    }

    public List<Trabajador> listarPorRol(Rol rol) {
        return repo.findByRol(rol);
    }

    public List<Trabajador> listarPorSede(Long sedeId) {
        return repo.findBySedeId(sedeId);
    }

    public List<Trabajador> listarPorRolYSede(Rol rol, Long sedeId) {
        return repo.findByRolAndSedeId(rol, sedeId);
    }

    public List<Trabajador> buscarPorNombre(String q) {
        return repo.findByNombreContainingIgnoreCase(q);
    }

    public List<Trabajador> buscar(String q) {
        return repo.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrUsernameContainingIgnoreCase(q, q, q);
    }

    public List<Trabajador> buscarPorTexto(String q) {
        return buscar(q);
    }

    public Optional<Trabajador> obtenerPorUsername(String username) {
        return repo.findByUsername(username);
    }

    public Trabajador crear(Trabajador t) {
        // Validaciones de campos obligatorios
        if (t.getCorreo() == null || t.getCorreo().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (t.getUsername() == null || t.getUsername().isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }
        if (t.getPassword() == null || t.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (t.getSede() == null || t.getSede().getId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        
        // Validaciones de unicidad
        if (repo.existsByCorreoIgnoreCase(t.getCorreo())) {
            throw new DataIntegrityViolationException("Ya existe un trabajador con ese correo");
        }
        if (repo.existsByUsernameIgnoreCase(t.getUsername())) {
            throw new DataIntegrityViolationException("Ya existe un trabajador con ese username");
        }
        
        // Verificar que la sede existe
        Sede sede = sedeRepository.findById(t.getSede().getId())
                .orElseThrow(() -> new IllegalArgumentException("La sede especificada no existe"));
        t.setSede(sede);
        
        return repo.save(t);
    }

    public Trabajador actualizar(Long id, Trabajador t) {
        return repo.findById(id).map(actual -> {
            if (t.getCorreo() != null) {
                if (repo.existsByCorreoIgnoreCaseAndIdNot(t.getCorreo(), id)) {
                    throw new DataIntegrityViolationException("Ya existe un trabajador con ese correo");
                }
                actual.setCorreo(t.getCorreo());
            }
            if (t.getUsername() != null) {
                // Verificar si ya existe otro trabajador con ese username
                if (repo.existsByUsernameIgnoreCaseAndIdNot(t.getUsername(), id)) {
                    throw new DataIntegrityViolationException("Ya existe un trabajador con ese username");
                }
                actual.setUsername(t.getUsername());
            }
            if (t.getPassword() != null && !t.getPassword().isBlank()) {
                actual.setPassword(t.getPassword());
            }
            if (t.getNombre() != null) actual.setNombre(t.getNombre());
            if (t.getRol() != null) actual.setRol(t.getRol());
            if (t.getSede() != null && t.getSede().getId() != null) {
                Sede sede = sedeRepository.findById(t.getSede().getId())
                        .orElseThrow(() -> new IllegalArgumentException("La sede especificada no existe"));
                actual.setSede(sede);
            }
            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("Trabajador no encontrado con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public Trabajador cambiarPassword(Long id, String nuevaPassword) {
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new IllegalArgumentException("La nueva contraseña es obligatoria");
        }
        return repo.findById(id).map(actual -> {
            actual.setPassword(nuevaPassword);
            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("Trabajador no encontrado con id " + id));
    }
}
