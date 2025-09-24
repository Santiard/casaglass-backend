package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Traslado;
import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import com.casaglass.casaglass_backend.repository.TrasladoDetalleRepository;
import com.casaglass.casaglass_backend.repository.TrasladoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;  // <-- IMPORTANTE

@Service
public class TrasladoDetalleService {

    private final TrasladoDetalleRepository detalleRepo;
    private final TrasladoRepository trasladoRepo;
    private final EntityManager em;

    public TrasladoDetalleService(TrasladoDetalleRepository detalleRepo,
                                  TrasladoRepository trasladoRepo,
                                  EntityManager em) {
        this.detalleRepo = detalleRepo;
        this.trasladoRepo = trasladoRepo;
        this.em = em;
    }

    // Renombrado/alias para que coincida con el controller
    public List<TrasladoDetalle> listar(Long trasladoId) {          // <-- NUEVO
        return detalleRepo.findByTrasladoId(trasladoId);
    }

    // Si quieres, conserva también el nombre anterior para no romper otros usos:
    public List<TrasladoDetalle> listarPorTraslado(Long trasladoId) {
        return detalleRepo.findByTrasladoId(trasladoId);
    }

    // Debe devolver Optional para que el controller pueda usar .map(...).orElse(...)
    public Optional<TrasladoDetalle> obtener(Long detalleId) {       // <-- CAMBIADO
        return detalleRepo.findById(detalleId);
    }

    @Transactional
    public TrasladoDetalle crear(Long trasladoId, TrasladoDetalle payload) {
        Traslado traslado = trasladoRepo.findById(trasladoId)
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado con id " + trasladoId));

        if (payload.getProducto() == null || payload.getProducto().getId() == null) {
            throw new IllegalArgumentException("Debe especificar producto.id");
        }
        if (payload.getCantidad() == null || payload.getCantidad() < 1) {
            throw new IllegalArgumentException("La cantidad debe ser >= 1");
        }

        payload.setTraslado(traslado);
        payload.setProducto(em.getReference(Producto.class, payload.getProducto().getId()));

        return detalleRepo.save(payload);
    }

    @Transactional
    public TrasladoDetalle actualizar(Long trasladoId, Long detalleId, TrasladoDetalle cambios) {
        TrasladoDetalle detalle = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado con id " + detalleId));

        if (!Objects.equals(detalle.getTraslado().getId(), trasladoId)) {
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");
        }

        if (cambios.getProducto() != null && cambios.getProducto().getId() != null) {
            detalle.setProducto(em.getReference(Producto.class, cambios.getProducto().getId()));
        }
        if (cambios.getCantidad() != null) {
            if (cambios.getCantidad() < 1) throw new IllegalArgumentException("La cantidad debe ser >= 1");
            detalle.setCantidad(cambios.getCantidad());
        }

        return detalleRepo.save(detalle);
    }

    @Transactional
    public void eliminar(Long trasladoId, Long detalleId) {
        TrasladoDetalle detalle = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado con id " + detalleId));

        if (!Objects.equals(detalle.getTraslado().getId(), trasladoId)) {
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");
        }

        detalleRepo.delete(detalle);
    }
}
