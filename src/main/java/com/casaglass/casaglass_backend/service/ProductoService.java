package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Categoria;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.repository.CategoriaRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepo;

    public ProductoService(ProductoRepository repo, CategoriaRepository categoriaRepo) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
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

    public List<Producto> listarPorCategoriaId(Long categoriaId) {
        return repo.findByCategoria_Id(categoriaId);
    }

    public List<Producto> buscar(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return repo.findAll();
        return repo.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q, q);
    }

    public Producto guardar(Producto p) {
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

    public Producto actualizar(Long id, Producto p) {
        return repo.findById(id).map(actual -> {
            try {
                // 🐛 DEBUG: Logging para identificar problemas
                System.out.println("=== ACTUALIZANDO PRODUCTO ===");
                System.out.println("ID: " + id);
                System.out.println("Version actual en DB: " + actual.getVersion());
                System.out.println("Version recibida: " + p.getVersion());
                System.out.println("Tipo recibido: " + p.getTipo());
                System.out.println("Color recibido: " + p.getColor());
                System.out.println("Categoria recibida: " + (p.getCategoria() != null ? p.getCategoria().getId() : "null"));
                
                // 🔧 NO TOCAR el version - Hibernate lo maneja automáticamente
                // actual.setVersion(p.getVersion()); // ❌ NO hacer esto
                
                actual.setPosicion(p.getPosicion());
                actual.setCodigo(p.getCodigo());
                actual.setNombre(p.getNombre());
                actual.setTipo(p.getTipo());
                actual.setColor(p.getColor());
                actual.setCantidad(p.getCantidad());
                actual.setCosto(p.getCosto());
                actual.setPrecio1(p.getPrecio1());
                actual.setPrecio2(p.getPrecio2());
                actual.setPrecio3(p.getPrecio3());
                actual.setPrecioEspecial(p.getPrecioEspecial());
                actual.setDescripcion(p.getDescripcion());

                // Actualizar categoría si se envía
                if (p.getCategoria() != null && p.getCategoria().getId() != null) {
                    Categoria cat = categoriaRepo.findById(p.getCategoria().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                    actual.setCategoria(cat);
                } else {
                    actual.setCategoria(null);
                }

                System.out.println("Producto antes de guardar: " + actual);
                Producto saved = repo.save(actual);
                System.out.println("Producto guardado exitosamente con version: " + saved.getVersion());
                return saved;
                
            } catch (Exception e) {
                System.err.println("ERROR al actualizar producto: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al actualizar producto: " + e.getMessage(), e);
            }
        }).orElseThrow(() -> new RuntimeException("Producto no encontrado con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public List<String> listarCategoriasTexto() {
        return repo.findDistinctCategorias();
    }
}
