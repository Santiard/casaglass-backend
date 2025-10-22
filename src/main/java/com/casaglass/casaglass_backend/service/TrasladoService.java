package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.TrasladoDetalleRepository;
import com.casaglass.casaglass_backend.repository.TrasladoRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.service.InventarioService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
// no LocalDateTime/LocalTime needed
import java.util.*;

@Service
public class TrasladoService {

    private final TrasladoRepository repo;
    private final TrasladoDetalleRepository detalleRepo;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final EntityManager em;

    public TrasladoService(TrasladoRepository repo,
                           TrasladoDetalleRepository detalleRepo,
                           SedeRepository sedeRepository,
                           ProductoRepository productoRepository,
                           InventarioService inventarioService,
                           EntityManager em) {
        this.repo = repo;
        this.detalleRepo = detalleRepo;
        this.sedeRepository = sedeRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.em = em;
    }

    /* ---------------- Consultas ---------------- */

    public List<Traslado> listar() { 
        return repo.findAllWithDetails(); // 🔁 Usar query optimizada con JOIN FETCH
    }

    public Optional<Traslado> obtener(Long id) { return repo.findById(id); }

    public List<Traslado> listarPorSedeOrigen(Long sedeOrigenId) { return repo.findBySedeOrigenId(sedeOrigenId); }

    public List<Traslado> listarPorSedeDestino(Long sedeDestinoId) { return repo.findBySedeDestinoId(sedeDestinoId); }

    public List<Traslado> listarPorFecha(LocalDate fecha) {
        return repo.findByFechaBetween(fecha, fecha);
    }

    public List<Traslado> listarPorRango(LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findByFechaBetween(desdeDia, hastaDia);
    }

    /* ---------------- Comandos (cabecera) ---------------- */

    @Transactional
    public Traslado crear(Traslado payload) {
        System.out.println("🔧 TrasladoService - Creando traslado");
        
        if (payload.getSedeOrigen() == null || payload.getSedeOrigen().getId() == null)
            throw new IllegalArgumentException("Debe especificar sedeOrigen.id");
        if (payload.getSedeDestino() == null || payload.getSedeDestino().getId() == null)
            throw new IllegalArgumentException("Debe especificar sedeDestino.id");
        if (Objects.equals(payload.getSedeOrigen().getId(), payload.getSedeDestino().getId()))
            throw new IllegalArgumentException("La sede de origen y destino no pueden ser la misma");

        // ARREGLO: Buscar entidades completas en lugar de usar proxies
        Sede sedeOrigen = sedeRepository.findById(payload.getSedeOrigen().getId())
            .orElseThrow(() -> new RuntimeException("Sede origen no encontrada con ID: " + payload.getSedeOrigen().getId()));
        Sede sedeDestino = sedeRepository.findById(payload.getSedeDestino().getId())
            .orElseThrow(() -> new RuntimeException("Sede destino no encontrada con ID: " + payload.getSedeDestino().getId()));
        
        payload.setSedeOrigen(sedeOrigen);
        payload.setSedeDestino(sedeDestino);

        if (payload.getFecha() == null) payload.setFecha(LocalDate.now());

        // detalles (si vienen en el payload)
        if (payload.getDetalles() != null) {
            for (TrasladoDetalle d : payload.getDetalles()) {
                if (d.getProducto() == null || d.getProducto().getId() == null)
                    throw new IllegalArgumentException("Cada detalle requiere producto.id");
                if (d.getCantidad() == null || d.getCantidad() < 1)
                    throw new IllegalArgumentException("Cada detalle requiere cantidad >= 1");
                
                // ARREGLO: Buscar producto completo en lugar de usar proxy
                Producto producto = productoRepository.findById(d.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + d.getProducto().getId()));
                
                d.setTraslado(payload);
                d.setProducto(producto);
            }
        }

        System.out.println("💾 Guardando traslado...");
        Traslado resultado = repo.save(payload);
        System.out.println("✅ Traslado creado exitosamente con ID: " + resultado.getId());
        
        // 🔄 ACTUALIZAR INVENTARIO: Restar de origen y sumar a destino
        actualizarInventarioTraslado(resultado);
        
        return resultado;
    }

    /**
     * Actualiza el inventario cuando se crea un traslado:
     * - Resta la cantidad de la sede origen
     * - Suma la cantidad a la sede destino
     */
    private void actualizarInventarioTraslado(Traslado traslado) {
        System.out.println("🔄 Actualizando inventario para traslado ID: " + traslado.getId());
        
        Long sedeOrigenId = traslado.getSedeOrigen().getId();
        Long sedeDestinoId = traslado.getSedeDestino().getId();
        
        for (TrasladoDetalle detalle : traslado.getDetalles()) {
            Long productoId = detalle.getProducto().getId();
            Integer cantidad = detalle.getCantidad();
            
            System.out.println("📦 Procesando producto ID: " + productoId + ", cantidad: " + cantidad);
            
            // 1. RESTAR de sede origen
            Optional<Inventario> inventarioOrigen = inventarioService.obtenerPorProductoYSede(productoId, sedeOrigenId);
            if (inventarioOrigen.isPresent()) {
                Inventario invOrigen = inventarioOrigen.get();
                int nuevaCantidadOrigen = invOrigen.getCantidad() - cantidad;
                
                if (nuevaCantidadOrigen < 0) {
                    throw new RuntimeException("No hay suficiente stock en sede origen. Disponible: " + 
                        invOrigen.getCantidad() + ", requerido: " + cantidad);
                }
                
                invOrigen.setCantidad(nuevaCantidadOrigen);
                inventarioService.actualizar(invOrigen.getId(), invOrigen);
                System.out.println("➖ Restado de origen (ID: " + sedeOrigenId + "): " + cantidad + 
                    ", nuevo total: " + nuevaCantidadOrigen);
            } else {
                throw new RuntimeException("No existe inventario del producto ID " + productoId + 
                    " en sede origen ID " + sedeOrigenId);
            }
            
            // 2. SUMAR a sede destino
            Optional<Inventario> inventarioDestino = inventarioService.obtenerPorProductoYSede(productoId, sedeDestinoId);
            if (inventarioDestino.isPresent()) {
                // Actualizar inventario existente
                Inventario invDestino = inventarioDestino.get();
                invDestino.setCantidad(invDestino.getCantidad() + cantidad);
                inventarioService.actualizar(invDestino.getId(), invDestino);
                System.out.println("➕ Sumado a destino (ID: " + sedeDestinoId + "): " + cantidad + 
                    ", nuevo total: " + invDestino.getCantidad());
            } else {
                // Crear nuevo inventario en sede destino
                Inventario nuevoInventario = new Inventario();
                nuevoInventario.setProducto(detalle.getProducto());
                nuevoInventario.setSede(traslado.getSedeDestino());
                nuevoInventario.setCantidad(cantidad);
                inventarioService.guardar(nuevoInventario);
                System.out.println("🆕 Creado inventario en destino (ID: " + sedeDestinoId + "): " + cantidad);
            }
        }
        
        System.out.println("✅ Inventario actualizado correctamente para traslado ID: " + traslado.getId());
    }

    @Transactional
    public Traslado actualizarCabecera(Long id, Traslado cambios) {
        return repo.findById(id).map(t -> {
            if (cambios.getSedeOrigen() != null && cambios.getSedeOrigen().getId() != null) {
                t.setSedeOrigen(em.getReference(Sede.class, cambios.getSedeOrigen().getId()));
            }
            if (cambios.getSedeDestino() != null && cambios.getSedeDestino().getId() != null) {
                t.setSedeDestino(em.getReference(Sede.class, cambios.getSedeDestino().getId()));
            }
            if (t.getSedeOrigen() != null && t.getSedeDestino() != null &&
                Objects.equals(t.getSedeOrigen().getId(), t.getSedeDestino().getId())) {
                throw new IllegalArgumentException("La sede de origen y destino no pueden ser la misma");
            }
            if (cambios.getFecha() != null) t.setFecha(cambios.getFecha());
            if (cambios.getTrabajadorConfirmacion() != null && cambios.getTrabajadorConfirmacion().getId() != null) {
                t.setTrabajadorConfirmacion(em.getReference(Trabajador.class, cambios.getTrabajadorConfirmacion().getId()));
            }
            if (cambios.getFechaConfirmacion() != null) {
                t.setFechaConfirmacion(cambios.getFechaConfirmacion());
            }
            return repo.save(t);
        }).orElseThrow(() -> new RuntimeException("Traslado no encontrado con id " + id));
    }

    @Transactional
    public Traslado confirmarLlegada(Long trasladoId, Long trabajadorId) {
        Traslado t = repo.findById(trasladoId)
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado"));
        t.setTrabajadorConfirmacion(em.getReference(Trabajador.class, trabajadorId));
        t.setFechaConfirmacion(LocalDate.now()); // ⚡ Establecer fecha de confirmación automáticamente
        return repo.save(t);
    }

    @Transactional
    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    /* ---------------- Detalles (anidados) ---------------- */

    public List<TrasladoDetalle> listarDetalles(Long trasladoId) {
        return detalleRepo.findByTrasladoId(trasladoId);
    }

    @Transactional
    public TrasladoDetalle agregarDetalle(Long trasladoId, TrasladoDetalle payload) {
        Traslado t = repo.findById(trasladoId)
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado"));
        if (payload.getProducto() == null || payload.getProducto().getId() == null)
            throw new IllegalArgumentException("producto.id requerido");
        if (payload.getCantidad() == null || payload.getCantidad() < 1)
            throw new IllegalArgumentException("cantidad debe ser >= 1");

        payload.setTraslado(t);
        payload.setProducto(em.getReference(Producto.class, payload.getProducto().getId()));
        return detalleRepo.save(payload);
    }

    @Transactional
    public TrasladoDetalle actualizarDetalle(Long trasladoId, Long detalleId, TrasladoDetalle payload) {
        TrasladoDetalle d = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
        if (!Objects.equals(d.getTraslado().getId(), trasladoId))
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");

        if (payload.getProducto() != null && payload.getProducto().getId() != null) {
            d.setProducto(em.getReference(Producto.class, payload.getProducto().getId()));
        }
        if (payload.getCantidad() != null) {
            if (payload.getCantidad() < 1) throw new IllegalArgumentException("cantidad debe ser >= 1");
            d.setCantidad(payload.getCantidad());
        }
        return detalleRepo.save(d);
    }

    @Transactional
    public void eliminarDetalle(Long trasladoId, Long detalleId) {
        TrasladoDetalle d = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
        if (!Objects.equals(d.getTraslado().getId(), trasladoId))
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");
        detalleRepo.delete(d);
    }
}
