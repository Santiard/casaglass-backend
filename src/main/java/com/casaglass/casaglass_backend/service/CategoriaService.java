package com.casaglass.casaglass_backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaglass.casaglass_backend.model.Categoria;
import com.casaglass.casaglass_backend.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    // Obtener todas
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    // Obtener una
    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    // Crear
    public Categoria crear(Categoria categoria) {
        if (categoriaRepository.existsByNombreIgnoreCase(categoria.getNombre())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
        return categoriaRepository.save(categoria);
    }

    // Actualizar
    public Categoria actualizar(Long id, Categoria categoria) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con id: " + id));

        existente.setNombre(categoria.getNombre());
        return categoriaRepository.save(existente);
    }

    // Eliminar
    public void eliminar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new IllegalArgumentException("Categoría no encontrada con id: " + id);
        }
        categoriaRepository.deleteById(id);
    }
}
