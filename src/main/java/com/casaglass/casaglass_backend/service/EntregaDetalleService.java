package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.EntregaDetalle;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.repository.EntregaDetalleRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EntregaDetalleService {

    @Autowired
    private EntregaDetalleRepository entregaDetalleRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    public List<EntregaDetalle> obtenerTodos() {
        return entregaDetalleRepository.findAll();
    }

    public Optional<EntregaDetalle> obtenerPorId(Long id) {
        return entregaDetalleRepository.findById(id);
    }

    public List<EntregaDetalle> obtenerPorEntrega(Long entregaId) {
        return entregaDetalleRepository.findByEntregaId(entregaId);
    }

    public List<EntregaDetalle> obtenerPorOrden(Long ordenId) {
        return entregaDetalleRepository.findByOrdenId(ordenId);
    }

    public Optional<EntregaDetalle> obtenerPorEntregaYOrden(Long entregaId, Long ordenId) {
        return entregaDetalleRepository.findByEntregaIdAndOrdenId(entregaId, ordenId);
    }

    public boolean existeOrdenEnEntrega(Long entregaId, Long ordenId) {
        return entregaDetalleRepository.existsByEntregaIdAndOrdenId(entregaId, ordenId);
    }

    public long contarDetallesPorEntrega(Long entregaId) {
        return entregaDetalleRepository.countByEntregaId(entregaId);
    }

    public EntregaDetalle crearDetalle(EntregaDetalle detalle) {
        // Validar que la orden existe
        if (detalle.getOrden() == null || !ordenRepository.existsById(detalle.getOrden().getId())) {
            throw new RuntimeException("La orden especificada no existe");
        }

        // Verificar que la orden no esté ya incluida en esta entrega
        if (detalle.getEntrega() != null && 
            entregaDetalleRepository.existsByEntregaIdAndOrdenId(detalle.getEntrega().getId(), detalle.getOrden().getId())) {
            throw new RuntimeException("La orden ya está incluida en esta entrega");
        }

        // Capturar datos de la orden en el momento de la entrega
        Orden orden = ordenRepository.findById(detalle.getOrden().getId())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        detalle.setMontoOrden(orden.getTotal());
        detalle.setNumeroOrden(orden.getNumero());
        detalle.setFechaOrden(orden.getFecha());

        EntregaDetalle detalleCreado = entregaDetalleRepository.save(detalle);

        // Marcar la orden como incluida en entrega
        orden.setIncluidaEntrega(true);
        ordenRepository.save(orden);

        return detalleCreado;
    }

    public EntregaDetalle actualizarDetalle(Long id, EntregaDetalle detalleActualizado) {
        return entregaDetalleRepository.findById(id)
                .map(detalle -> {
                    // Solo permitir actualizar ciertos campos, los datos de snapshot no cambian
                    if (detalleActualizado.getObservaciones() != null) {
                        detalle.setObservaciones(detalleActualizado.getObservaciones());
                    }

                    return entregaDetalleRepository.save(detalle);
                })
                .orElseThrow(() -> new RuntimeException("Detalle de entrega no encontrado con id: " + id));
    }

    public void eliminarDetalle(Long id) {
        EntregaDetalle detalle = entregaDetalleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Detalle de entrega no encontrado con id: " + id));

        // Desmarcar la orden como no incluida en entrega
        if (detalle.getOrden() != null) {
            Orden orden = detalle.getOrden();
            orden.setIncluidaEntrega(false);
            ordenRepository.save(orden);
        }

        entregaDetalleRepository.deleteById(id);
    }

    public void eliminarDetallesPorEntrega(Long entregaId) {
        List<EntregaDetalle> detalles = entregaDetalleRepository.findByEntregaId(entregaId);
        
        // Desmarcar todas las órdenes como no incluidas
        for (EntregaDetalle detalle : detalles) {
            if (detalle.getOrden() != null) {
                Orden orden = detalle.getOrden();
                orden.setIncluidaEntrega(false);
                ordenRepository.save(orden);
            }
        }

        entregaDetalleRepository.deleteByEntregaId(entregaId);
    }

    public boolean validarOrdenParaEntrega(Long ordenId) {
        Optional<Orden> orden = ordenRepository.findById(ordenId);
        return orden.isPresent() && !orden.get().isIncluidaEntrega();
    }

    public Double calcularMontoTotalEntrega(Long entregaId) {
        return entregaDetalleRepository.calcularMontoTotalPorEntrega(entregaId);
    }
}