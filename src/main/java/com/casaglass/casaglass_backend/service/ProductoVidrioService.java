package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.repository.ProductoVidrioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoVidrioService {

    private final ProductoVidrioRepository repo;

    public ProductoVidrioService(ProductoVidrioRepository repo) {
        this.repo = repo;
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

    public List<ProductoVidrio> listarPorLaminas(Integer laminas) {
        return repo.findByLaminas(laminas);
    }

    public ProductoVidrio guardar(ProductoVidrio p) {
        return repo.save(p);
    }

    public ProductoVidrio actualizar(Long id, ProductoVidrio p) {
        return repo.findById(id).map(actual -> {
            // Campos heredados de Producto
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

            // Campos específicos de ProductoVidrio
            actual.setMm(p.getMm());
            actual.setM1m2(p.getM1m2());
            actual.setLaminas(p.getLaminas());

            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("ProductoVidrio no encontrado con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}