package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SedeService {

    private final SedeRepository repo;

    public SedeService(SedeRepository repo) {
        this.repo = repo;
    }

    public List<Sede> listar() {
        return repo.findAll();
    }

    public Optional<Sede> obtenerPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<Sede> obtenerPorNombre(String nombre) {
        return repo.findByNombreIgnoreCase(nombre);
    }

    public List<Sede> buscarPorNombre(String q) {
        return repo.findByNombreContainingIgnoreCase(q);
    }

    public List<Sede> buscarPorCiudad(String ciudad) {
        return repo.findByCiudadContainingIgnoreCase(ciudad);
    }

    public List<Sede> buscar(String q) {
        return repo.findByNombreContainingIgnoreCaseOrCiudadContainingIgnoreCase(q, q);
    }

    public Sede crear(Sede s) {
        // nombre es único
        if (repo.existsByNombreIgnoreCase(s.getNombre())) {
            throw new DataIntegrityViolationException("Ya existe una sede con ese nombre");
        }
        return repo.save(s);
    }

    public Sede actualizar(Long id, Sede s) {
        return repo.findById(id).map(actual -> {
            // Verificar duplicado de nombre en otros registros
            if (s.getNombre() != null &&
                repo.existsByNombreIgnoreCaseAndIdNot(s.getNombre(), id)) {
                throw new DataIntegrityViolationException("Ya existe una sede con ese nombre");
            }
            if (s.getNombre() != null) actual.setNombre(s.getNombre());
            if (s.getDireccion() != null) actual.setDireccion(s.getDireccion());
            if (s.getCiudad() != null) actual.setCiudad(s.getCiudad());
            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("Sede no encontrada con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
