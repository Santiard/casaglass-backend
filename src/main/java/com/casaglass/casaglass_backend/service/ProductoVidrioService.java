package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Categoria;
import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.repository.CategoriaRepository;
import com.casaglass.casaglass_backend.repository.ProductoVidrioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoVidrioService {

    private final ProductoVidrioRepository repo;
    private final CategoriaRepository categoriaRepo;

    public ProductoVidrioService(ProductoVidrioRepository repo, CategoriaRepository categoriaRepo) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
    }

    public List<ProductoVidrio> listar() {
        return repo.findAll();
    }

    public Optional<ProductoVidrio> obtenerPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<ProductoVidrio> obtenerPorCodigo(String codigo) {
        return repo.findByCodigo(codigo);
    }

    public List<ProductoVidrio> buscar(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return repo.findAll();
        return repo.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q, q);
    }

    public List<ProductoVidrio> listarPorMm(Double mm) {
        return repo.findByMm(mm);
    }


    public List<ProductoVidrio> listarPorCategoriaId(Long categoriaId) {
        return repo.findByCategoria_Id(categoriaId);
    }

    public ProductoVidrio guardar(ProductoVidrio p) {
        // Validar categoría si viene con ID
        if (p.getCategoria() != null && p.getCategoria().getId() != null) {
            Categoria cat = categoriaRepo.findById(p.getCategoria().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            p.setCategoria(cat);
        } else {
            p.setCategoria(null);
        }
        return repo.save(p);
    }

    public ProductoVidrio actualizar(Long id, ProductoVidrio p) {
        return repo.findById(id).map(actual -> {
            // Campos heredados de Producto
            actual.setPosicion(p.getPosicion());
            actual.setCodigo(p.getCodigo());
            actual.setNombre(p.getNombre());
            actual.setColor(p.getColor());
            actual.setCantidad(p.getCantidad());
            actual.setCosto(p.getCosto());
            actual.setPrecio1(p.getPrecio1());
            actual.setPrecio2(p.getPrecio2());
            actual.setPrecio3(p.getPrecio3());
            actual.setDescripcion(p.getDescripcion());

            // Actualizar categoría si se envía
            if (p.getCategoria() != null && p.getCategoria().getId() != null) {
                Categoria cat = categoriaRepo.findById(p.getCategoria().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                actual.setCategoria(cat);
            } else {
                actual.setCategoria(null);
            }

            // Campos específicos de ProductoVidrio
            actual.setMm(p.getMm());
            actual.setM1(p.getM1());
            actual.setM2(p.getM2());

            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("ProductoVidrio no encontrado con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}