package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.TrasladoDetalleRepository;
import com.casaglass.casaglass_backend.repository.TrasladoRepository;
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
    private final EntityManager em;

    public TrasladoService(TrasladoRepository repo,
                           TrasladoDetalleRepository detalleRepo,
                           EntityManager em) {
        this.repo = repo;
        this.detalleRepo = detalleRepo;
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
        if (payload.getSedeOrigen() == null || payload.getSedeOrigen().getId() == null)
            throw new IllegalArgumentException("Debe especificar sedeOrigen.id");
        if (payload.getSedeDestino() == null || payload.getSedeDestino().getId() == null)
            throw new IllegalArgumentException("Debe especificar sedeDestino.id");
        if (Objects.equals(payload.getSedeOrigen().getId(), payload.getSedeDestino().getId()))
            throw new IllegalArgumentException("La sede de origen y destino no pueden ser la misma");

        // referencias ligeras (evita SELECT completos)
        payload.setSedeOrigen(em.getReference(Sede.class, payload.getSedeOrigen().getId()));
        payload.setSedeDestino(em.getReference(Sede.class, payload.getSedeDestino().getId()));

    if (payload.getFecha() == null) payload.setFecha(LocalDate.now());

        // detalles (si vienen en el payload)
        if (payload.getDetalles() != null) {
            for (TrasladoDetalle d : payload.getDetalles()) {
                if (d.getProducto() == null || d.getProducto().getId() == null)
                    throw new IllegalArgumentException("Cada detalle requiere producto.id");
                if (d.getCantidad() == null || d.getCantidad() < 1)
                    throw new IllegalArgumentException("Cada detalle requiere cantidad >= 1");
                d.setTraslado(payload);
                d.setProducto(em.getReference(Producto.class, d.getProducto().getId()));
            }
        }

        return repo.save(payload);
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
