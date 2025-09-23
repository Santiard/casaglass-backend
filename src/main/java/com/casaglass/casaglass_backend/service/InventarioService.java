package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InventarioService {

    private final InventarioRepository repo;
    private final EntityManager em;

    public InventarioService(InventarioRepository repo, EntityManager em) {
        this.repo = repo;
        this.em = em;
    }

    public List<Inventario> listar() {
        return repo.findAll();
    }

    public Optional<Inventario> obtenerPorId(Long id) {
        return repo.findById(id);
    }

    public List<Inventario> listarPorProducto(Long productoId) {
        return repo.findByProductoId(productoId);
    }

    public List<Inventario> listarPorSede(Long sedeId) {
        return repo.findBySedeId(sedeId);
    }

    public Optional<Inventario> obtenerPorProductoYSede(Long productoId, Long sedeId) {
        return repo.findByProductoIdAndSedeId(productoId, sedeId);
    }

    @Transactional
    public Inventario guardar(Inventario payload) {
        // Esperamos payload con producto.id y sede.id
        Long productoId = payload.getProducto() != null ? payload.getProducto().getId() : null;
        Long sedeId = payload.getSede() != null ? payload.getSede().getId() : null;
        if (productoId == null || sedeId == null) {
            throw new IllegalArgumentException("Se requieren producto.id y sede.id");
        }

        // Usamos referencias ligeras para evitar consultas completas
        Producto productoRef = em.getReference(Producto.class, productoId);
        Sede sedeRef = em.getReference(Sede.class, sedeId);

        Inventario inv = new Inventario();
        inv.setProducto(productoRef);
        inv.setSede(sedeRef);
        inv.setCantidad(payload.getCantidad() == null ? 0 : payload.getCantidad());

        return repo.save(inv); // si duplica (producto,sede), saltará la uniqueConstraint -> DataIntegrityViolationException
    }

    @Transactional
    public Inventario actualizar(Long id, Inventario payload) {
        return repo.findById(id).map(actual -> {
            // Permitir cambiar cantidad; cambiar producto/sede es posible,
            // pero puede chocar con la uniqueConstraint si ya existe esa combinación.
            if (payload.getProducto() != null && payload.getProducto().getId() != null) {
                actual.setProducto(em.getReference(Producto.class, payload.getProducto().getId()));
            }
            if (payload.getSede() != null && payload.getSede().getId() != null) {
                actual.setSede(em.getReference(Sede.class, payload.getSede().getId()));
            }
            if (payload.getCantidad() != null) {
                actual.setCantidad(payload.getCantidad());
            }
            return repo.save(actual);
        }).orElseThrow(() -> new RuntimeException("Inventario no encontrado con id " + id));
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}