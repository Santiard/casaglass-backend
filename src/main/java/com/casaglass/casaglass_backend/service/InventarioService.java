package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.InventarioProductoDTO;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.casaglass.casaglass_backend.dto.InventarioProductoDTO;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Transactional(readOnly = true)
    public List<Inventario> listar() {
        return repo.findAllWithDetails(); // 🔧 Usar método con FETCH JOINS
    }

    @Transactional(readOnly = true)
    public Optional<Inventario> obtenerPorId(Long id) {
        return repo.findByIdWithDetails(id); // 🔧 Usar método con FETCH JOINS
    }

    @Transactional(readOnly = true)
    public List<Inventario> listarPorProducto(Long productoId) {
        return repo.findByProductoId(productoId);
    }

    @Transactional(readOnly = true)
    public List<Inventario> listarPorSede(Long sedeId) {
        return repo.findBySedeId(sedeId);
    }

    @Transactional(readOnly = true)
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

        // Buscar si ya existe inventario para esta combinación producto-sede
        Optional<Inventario> inventarioExistente = obtenerPorProductoYSede(productoId, sedeId);
        
        if (inventarioExistente.isPresent()) {
            // ACTUALIZAR inventario existente
            Inventario inv = inventarioExistente.get();
            inv.setCantidad(payload.getCantidad() == null ? 0 : payload.getCantidad());
            return repo.save(inv);
        } else {
            // CREAR nuevo inventario
            Producto productoRef = em.getReference(Producto.class, productoId);
            Sede sedeRef = em.getReference(Sede.class, sedeId);

            Inventario inv = new Inventario();
            inv.setProducto(productoRef);
            inv.setSede(sedeRef);
            inv.setCantidad(payload.getCantidad() == null ? 0 : payload.getCantidad());

            return repo.save(inv);
        }
    }

    /**
     * 📦 ACTUALIZAR INVENTARIO PARA VENTAS
     * Método específico para actualizar inventario en operaciones de venta/anulación
     * Garantiza que no habrá duplicados
     */
    @Transactional
    public Inventario actualizarInventarioVenta(Long productoId, Long sedeId, int nuevaCantidad) {
        if (productoId == null || sedeId == null) {
            throw new IllegalArgumentException("Se requieren producto ID y sede ID");
        }

        // Buscar inventario existente
        Optional<Inventario> inventarioOpt = obtenerPorProductoYSede(productoId, sedeId);
        
        if (inventarioOpt.isPresent()) {
            // ACTUALIZAR inventario existente
            Inventario inventario = inventarioOpt.get();
            inventario.setCantidad(nuevaCantidad);
            return repo.save(inventario);
        } else {
            // CREAR nuevo inventario solo si no existe
            Producto productoRef = em.getReference(Producto.class, productoId);
            Sede sedeRef = em.getReference(Sede.class, sedeId);

            Inventario nuevoInventario = new Inventario();
            nuevoInventario.setProducto(productoRef);
            nuevoInventario.setSede(sedeRef);
            nuevoInventario.setCantidad(nuevaCantidad);

            return repo.save(nuevoInventario);
        }
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

    @Transactional(readOnly = true) // 🔧 Cambio a readOnly para consultas
    public List<InventarioProductoDTO> listarInventarioAgrupado() {
        List<Inventario> inventarios = repo.findAllWithDetails(); // 🔧 Usar método con FETCH JOINS
        Map<Long, InventarioProductoDTO> mapa = new HashMap<>();

        for (Inventario inv : inventarios) {
            Long productoId = inv.getProducto().getId();
            String productoNombre = inv.getProducto().getNombre();
            String sedeNombre = inv.getSede().getNombre().toLowerCase();

            InventarioProductoDTO dto = mapa.computeIfAbsent(productoId,
                id -> new InventarioProductoDTO(id, productoNombre, 0, 0, 0)
            );

            if (sedeNombre.contains("insula")) dto.setCantidadInsula(inv.getCantidad());
            if (sedeNombre.contains("centro")) dto.setCantidadCentro(inv.getCantidad());
            if (sedeNombre.contains("patios")) dto.setCantidadPatios(inv.getCantidad());
        }

        return new ArrayList<>(mapa.values());
    }
}