package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.InventarioActualizarDTO;
import com.casaglass.casaglass_backend.dto.InventarioProductoDTO;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class InventarioService {

    private final InventarioRepository repo;
    private final EntityManager em;
    private final SedeRepository sedeRepo;

    public InventarioService(InventarioRepository repo, EntityManager em, SedeRepository sedeRepo) {
        this.repo = repo;
        this.em = em;
        this.sedeRepo = sedeRepo;
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

    /**
     * 🔒 OBTENER INVENTARIO CON LOCK OPTIMISTA
     * 
     * Usa @Version en la entidad Inventario para control de concurrencia
     * No bloquea el registro → operaciones concurrentes sin esperas
     * Si hay conflicto real (muy raro) → OptimisticLockException
     * 
     * @deprecated Nombre obsoleto, ahora usa lock optimista. Usar obtenerPorProductoYSede() directamente.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Optional<Inventario> obtenerPorProductoYSedeConLock(Long productoId, Long sedeId) {
        // Simplemente redirige al método normal (ya usa lock optimista vía @Version)
        return obtenerPorProductoYSede(productoId, sedeId);
    }

    @Transactional
    public Inventario guardar(Inventario payload) {
        // Esperamos payload con producto.id y sede.id
        Long productoId = payload.getProducto() != null ? payload.getProducto().getId() : null;
        Long sedeId = payload.getSede() != null ? payload.getSede().getId() : null;
        if (productoId == null || sedeId == null) {
            throw new IllegalArgumentException("Se requieren producto.id y sede.id");
        }

        try {
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
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario del producto ID %d. Por favor, intente nuevamente.", productoId)
            );
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario del producto ID %d. Por favor, intente nuevamente.", productoId)
            );
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

        try {
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
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario del producto ID %d. Por favor, intente nuevamente.", productoId)
            );
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario del producto ID %d. Por favor, intente nuevamente.", productoId)
            );
        }
    }

    @Transactional
    public Inventario actualizar(Long id, Inventario payload) {
        try {
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
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario ID %d. Por favor, intente nuevamente.", id)
            );
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario ID %d. Por favor, intente nuevamente.", id)
            );
        }
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

    /**
     * 📦 ACTUALIZAR INVENTARIO DE UN PRODUCTO EN LAS 3 SEDES
     * Actualiza el inventario en Insula, Centro y Patios con los valores enviados
     * 
     * @param productoId ID del producto
     * @param dto DTO con las cantidades para las 3 sedes
     * @return Lista de inventarios actualizados
     */
    @Transactional
    public List<Inventario> actualizarInventarioPorProducto(Long productoId, InventarioActualizarDTO dto) {
        // Obtener IDs de las 3 sedes
        Long insulaId = obtenerSedeId("insula");
        Long centroId = obtenerSedeId("centro");
        Long patiosId = obtenerSedeId("patios");
        if (insulaId == null || centroId == null || patiosId == null) {
            throw new IllegalArgumentException("No se encontraron las 3 sedes (Insula, Centro, Patios)");
        }
        // Permitir valores negativos (ventas anticipadas) - usar 0 como default solo si es null
        Integer cantidadInsula = dto.getCantidadInsula() != null ? dto.getCantidadInsula() : 0;
        Integer cantidadCentro = dto.getCantidadCentro() != null ? dto.getCantidadCentro() : 0;
        Integer cantidadPatios = dto.getCantidadPatios() != null ? dto.getCantidadPatios() : 0;
        List<Inventario> inventariosActualizados = new ArrayList<>();
        // Actualizar o crear inventario para cada sede
        inventariosActualizados.add(actualizarInventarioSede(productoId, insulaId, cantidadInsula));
        inventariosActualizados.add(actualizarInventarioSede(productoId, centroId, cantidadCentro));
        inventariosActualizados.add(actualizarInventarioSede(productoId, patiosId, cantidadPatios));
        return inventariosActualizados;
    }
    
    /**
     * Actualizar o crear inventario para un producto en una sede específica
     */
    private Inventario actualizarInventarioSede(Long productoId, Long sedeId, Integer cantidad) {
        Optional<Inventario> inventarioOpt = obtenerPorProductoYSede(productoId, sedeId);
        
        if (inventarioOpt.isPresent()) {
            // Actualizar inventario existente
            Inventario inventario = inventarioOpt.get();
            inventario.setCantidad(cantidad);
            return repo.save(inventario);
        } else {
            // Crear nuevo inventario
            Producto productoRef = em.getReference(Producto.class, productoId);
            Sede sedeRef = em.getReference(Sede.class, sedeId);
            
            Inventario nuevoInventario = new Inventario();
            nuevoInventario.setProducto(productoRef);
            nuevoInventario.setSede(sedeRef);
            nuevoInventario.setCantidad(cantidad);
            return repo.save(nuevoInventario);
        }
    }
    
    /**
     * Obtener ID de sede por nombre (búsqueda parcial, case-insensitive)
     */
    private Long obtenerSedeId(String nombreSede) {
        return sedeRepo.findByNombreContainingIgnoreCase(nombreSede)
            .stream()
            .findFirst()
            .map(Sede::getId)
            .orElse(null);
    }
}