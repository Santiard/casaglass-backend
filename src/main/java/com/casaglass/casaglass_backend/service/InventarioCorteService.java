package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.InventarioCorteDTO;
import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.model.InventarioCorte;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.InventarioCorteRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InventarioCorteService {

    private final InventarioCorteRepository repository;
    private final EntityManager entityManager;

    public InventarioCorteService(InventarioCorteRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    // Operaciones básicas CRUD
    public List<InventarioCorte> listar() {
        return repository.findAll();
    }

    public Optional<InventarioCorte> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    public Optional<InventarioCorte> obtenerPorCorteYSede(Long corteId, Long sedeId) {
        return repository.findByCorteIdAndSedeId(corteId, sedeId);
    }

    @Transactional
    public InventarioCorte guardar(InventarioCorte inventarioCorte) {
        // Validaciones básicas
        if (inventarioCorte.getCorte() == null || inventarioCorte.getCorte().getId() == null) {
            throw new IllegalArgumentException("El corte es obligatorio");
        }
        if (inventarioCorte.getSede() == null || inventarioCorte.getSede().getId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        // ✅ Permitir cantidades negativas (ventas anticipadas, como en productos normales)
        if (inventarioCorte.getCantidad() == null) {
            throw new IllegalArgumentException("La cantidad es obligatoria");
        }

        // Verificar si ya existe inventario para este corte en esta sede
        Optional<InventarioCorte> existente = repository.findByCorteIdAndSedeId(
                inventarioCorte.getCorte().getId(), 
                inventarioCorte.getSede().getId()
        );

        if (existente.isPresent() && inventarioCorte.getId() == null) {
            throw new IllegalArgumentException("Ya existe inventario para este corte en esta sede");
        }

        // Usar referencias para evitar SELECT innecesarios
        if (inventarioCorte.getId() == null) {
            inventarioCorte.setCorte(entityManager.getReference(Corte.class, inventarioCorte.getCorte().getId()));
            inventarioCorte.setSede(entityManager.getReference(Sede.class, inventarioCorte.getSede().getId()));
        }

        return repository.save(inventarioCorte);
    }

    @Transactional
    public InventarioCorte actualizar(Long id, InventarioCorte inventarioActualizado) {
        return repository.findById(id)
                .map(inventarioExistente -> {
                    inventarioExistente.setCantidad(inventarioActualizado.getCantidad());
                    return repository.save(inventarioExistente);
                })
                .orElseThrow(() -> new RuntimeException("Inventario de corte no encontrado con ID: " + id));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Inventario de corte no encontrado con ID: " + id);
        }
        repository.deleteById(id);
    }

    // Búsquedas especializadas
    public List<InventarioCorte> listarPorCorte(Long corteId) {
        return repository.findByCorteId(corteId);
    }

    public List<InventarioCorte> listarPorSede(Long sedeId) {
        return repository.findBySedeId(sedeId);
    }

    public List<InventarioCorte> listarConStock() {
        return repository.findAllWithStock();
    }

    public List<InventarioCorte> listarSinStock() {
        return repository.findAllWithoutStock();
    }

    public List<InventarioCorte> listarPorSedeConStock(Long sedeId) {
        return repository.findBySedeIdWithStock(sedeId);
    }

    public List<InventarioCorte> listarPorCorteConStock(Long corteId) {
        return repository.findByCorteIdWithStock(corteId);
    }

    public List<InventarioCorte> listarPorRangoCantidad(Integer cantidadMin, Integer cantidadMax) {
        return repository.findByCantidadRange(cantidadMin, cantidadMax);
    }

    // Métodos de utilidad
    public Integer obtenerStockTotalPorCorte(Long corteId) {
        Integer total = repository.getTotalStockByCorteId(corteId);
        return total != null ? total : 0;
    }

    public Integer obtenerStockTotalPorSede(Long sedeId) {
        Integer total = repository.getTotalStockBySedeId(sedeId);
        return total != null ? total : 0;
    }

    public List<Long> obtenerSedesConStock(Long corteId) {
        return repository.findSedesWithStockByCorteId(corteId);
    }

    @Transactional
    public InventarioCorte actualizarStock(Long corteId, Long sedeId, Double nuevaCantidad) {
        Optional<InventarioCorte> inventarioOpt = repository.findByCorteIdAndSedeId(corteId, sedeId);
        
        if (inventarioOpt.isPresent()) {
            InventarioCorte inventario = inventarioOpt.get();
            inventario.setCantidad(nuevaCantidad);
            return repository.save(inventario);
        } else {
            // Crear nuevo inventario si no existe
            InventarioCorte nuevoInventario = new InventarioCorte();
            nuevoInventario.setCorte(entityManager.getReference(Corte.class, corteId));
            nuevoInventario.setSede(entityManager.getReference(Sede.class, sedeId));
            nuevoInventario.setCantidad(nuevaCantidad);
            return repository.save(nuevoInventario);
        }
    }

    @Transactional
    public InventarioCorte incrementarStock(Long corteId, Long sedeId, Double cantidad) {
        Optional<InventarioCorte> inventarioOpt = repository.findByCorteIdAndSedeId(corteId, sedeId);
        
        if (inventarioOpt.isPresent()) {
            InventarioCorte inventario = inventarioOpt.get();
            inventario.setCantidad(inventario.getCantidad() + cantidad);
            return repository.save(inventario);
        } else {
            return actualizarStock(corteId, sedeId, cantidad);
        }
    }

    @Transactional
    public InventarioCorte decrementarStock(Long corteId, Long sedeId, Double cantidad) {
        Optional<InventarioCorte> inventarioOpt = repository.findByCorteIdAndSedeId(corteId, sedeId);
        
        if (inventarioOpt.isPresent()) {
            InventarioCorte inventario = inventarioOpt.get();
            double nuevaCantidad = inventario.getCantidad() - cantidad;
            // ✅ Permitir valores negativos (ventas anticipadas, como en productos normales)
            // Si el inventario está en 0 y se vende, queda en negativo (se puede reponer después)
            inventario.setCantidad(nuevaCantidad);
            return repository.save(inventario);
        } else {
            // ✅ Si no existe inventario, crearlo con cantidad negativa (venta anticipada)
            // Esto permite vender cortes que fueron creados pero no tienen inventario inicial
            InventarioCorte nuevoInventario = new InventarioCorte();
            nuevoInventario.setCorte(entityManager.getReference(Corte.class, corteId));
            nuevoInventario.setSede(entityManager.getReference(Sede.class, sedeId));
            nuevoInventario.setCantidad(-cantidad); // Cantidad negativa = venta anticipada
            return repository.save(nuevoInventario);
        }
    }
    
@Transactional
public List<InventarioCorteDTO> listarInventarioCortesAgrupado() {
    List<InventarioCorte> inventarios = repository.findAll();
    Map<Long, InventarioCorteDTO> mapa = new HashMap<>();

    for (InventarioCorte inv : inventarios) {
        Long corteId = inv.getCorte().getId();
        String nombre = inv.getCorte().getNombre();
        Double largoCm = inv.getCorte().getLargoCm();
        Double precio = inv.getCorte().getPrecio1();
        String sede = inv.getSede().getNombre().toLowerCase();

        InventarioCorteDTO dto = mapa.computeIfAbsent(corteId,
            id -> new InventarioCorteDTO(id, nombre, largoCm, precio, 0.0, 0.0, 0.0)
        );

        if (sede.contains("insula")) dto.setCantidadInsula(inv.getCantidad());
        if (sede.contains("centro")) dto.setCantidadCentro(inv.getCantidad());
        if (sede.contains("patios")) dto.setCantidadPatios(inv.getCantidad());
    }

    return new ArrayList<>(mapa.values());
}
}