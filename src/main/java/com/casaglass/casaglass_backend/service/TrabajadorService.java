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
        return repo.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCase(q, q);
    }

    public Trabajador crear(Trabajador t) {
        if (t.getCorreo() == null || t.getCorreo().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (t.getSede() == null || t.getSede().getId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        if (repo.existsByCorreoIgnoreCase(t.getCorreo())) {
            throw new DataIntegrityViolationException("Ya existe un trabajador con ese correo");
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
}
