package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository repo;

    public ProductoService(ProductoRepository repo) {
        this.repo = repo;
    }

    public List<Producto> listar() {
        return repo.findAll();
    }

    public Optional<Producto> obtenerPorId(Long id) {
        return repo.findById(id);
    }

    public Optional<Producto> obtenerPorCodigo(String codigo) {
        return repo.findByCodigo(codigo);
    }

    public List<Producto> listarPorCategoria(String categoria) {
        return repo.findByCategoriaIgnoreCase(categoria);
    }

    public List<Producto> buscar(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return repo.findAll();
        return repo.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q, q);
    }

    public Producto guardar(Producto p) {
        return repo.save(p);
    }

    public Producto actualizar(Long id, Producto p) {
        return repo.findById(id).map(actual -> {
            actual.setPosicion(p.getPosicion());
            actual.setCategoria(p.getCategoria());
            actual.setCodigo(p.getCodigo());
            actual.setNombre(p.getNombre());
            actual.setColor(p.getColor());
            actual.setCantidad(p.getCantidad());
            actual.setCosto(p.getCosto());
            actual.setPrecio1(p.getPrecio1());
            actual.setPrecio2(p.getPrecio2());
            actual.setPrecio3(p.getPrecio3());
            actual.setPrecioEspecial(p.getPrecioEspecial());
            actual.setDescripcion(p.getDescripcion());
            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("Producto no encontrado con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
    public List<String> listarCategorias() {
    return repo.findDistinctCategorias();
    }
}
