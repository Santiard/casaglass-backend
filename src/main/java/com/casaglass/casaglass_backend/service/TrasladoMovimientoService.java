package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.TrasladoMovimientoDTO;
import com.casaglass.casaglass_backend.model.Traslado;
import com.casaglass.casaglass_backend.repository.TrasladoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio especializado para obtener movimientos de traslado
 * optimizados para el frontend con información consolidada
 */
@Service
@Transactional(readOnly = true)
public class TrasladoMovimientoService {

    private final TrasladoRepository trasladoRepository;
    private final TrasladoService trasladoService;

    public TrasladoMovimientoService(TrasladoRepository trasladoRepository, TrasladoService trasladoService) {
        this.trasladoRepository = trasladoRepository;
        this.trasladoService = trasladoService;
    }

    /**
     * Obtiene todos los movimientos de traslado con información consolidada
     */
    public List<TrasladoMovimientoDTO> obtenerMovimientos() {
        List<Traslado> traslados = trasladoRepository.findAllWithDetails();
        return traslados.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene movimientos filtrados por rango de fechas
     */
    public List<TrasladoMovimientoDTO> obtenerMovimientosPorRango(LocalDate desde, LocalDate hasta) {
        List<Traslado> traslados = trasladoRepository.findByFechaBetween(desde, hasta);
        return traslados.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene movimientos de una sede específica (origen o destino)
     */
    public List<TrasladoMovimientoDTO> obtenerMovimientosPorSede(Long sedeId) {
        List<Traslado> traslados = trasladoRepository.findBySedeOrigenIdOrSedeDestinoId(sedeId, sedeId);
        return traslados.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene movimientos pendientes de confirmación
     */
    public List<TrasladoMovimientoDTO> obtenerMovimientosPendientes() {
        List<Traslado> traslados = trasladoRepository.findByTrabajadorConfirmacionIsNull();
        return traslados.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene movimientos confirmados
     */
    public List<TrasladoMovimientoDTO> obtenerMovimientosConfirmados() {
        List<Traslado> traslados = trasladoRepository.findByTrabajadorConfirmacionIsNotNull();
        return traslados.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Confirma un traslado y devuelve el DTO actualizado
     */
    @Transactional
    public TrasladoMovimientoDTO confirmarTraslado(Long trasladoId, Long trabajadorId) {
        // Usar el servicio existente para confirmar
        Traslado trasladoConfirmado = trasladoService.confirmarLlegada(trasladoId, trabajadorId);
        
        // Recargar el traslado con todas las relaciones para el DTO
        Traslado trasladoCompleto = trasladoRepository.findById(trasladoConfirmado.getId())
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado después de confirmación"));
                
        return convertirADTO(trasladoCompleto);
    }

    /**
     * Convierte entidad Traslado a DTO optimizado para frontend
     */
    private TrasladoMovimientoDTO convertirADTO(Traslado traslado) {
        // Sede origen
        TrasladoMovimientoDTO.SedeSimpleDTO sedeOrigen = null;
        if (traslado.getSedeOrigen() != null) {
            sedeOrigen = new TrasladoMovimientoDTO.SedeSimpleDTO(
                    traslado.getSedeOrigen().getId(),
                    traslado.getSedeOrigen().getNombre()
            );
        }

        // Sede destino
        TrasladoMovimientoDTO.SedeSimpleDTO sedeDestino = null;
        if (traslado.getSedeDestino() != null) {
            sedeDestino = new TrasladoMovimientoDTO.SedeSimpleDTO(
                    traslado.getSedeDestino().getId(),
                    traslado.getSedeDestino().getNombre()
            );
        }

        // Trabajador de confirmación
        TrasladoMovimientoDTO.TrabajadorSimpleDTO trabajadorConfirmacion = null;
        if (traslado.getTrabajadorConfirmacion() != null) {
            trabajadorConfirmacion = new TrasladoMovimientoDTO.TrabajadorSimpleDTO(
                    traslado.getTrabajadorConfirmacion().getId(),
                    traslado.getTrabajadorConfirmacion().getNombre()
            );
        }

        // Detalles del traslado
        List<TrasladoMovimientoDTO.TrasladoDetalleSimpleDTO> detalles = null;
        if (traslado.getDetalles() != null) {
            detalles = traslado.getDetalles().stream()
                    .map(detalle -> {
                        // Información del producto
                        TrasladoMovimientoDTO.ProductoSimpleDTO producto = null;
                        if (detalle.getProducto() != null) {
                            String categoria = detalle.getProducto().getCategoria() != null ?
                                    detalle.getProducto().getCategoria().getNombre() : null;
                            
                            producto = new TrasladoMovimientoDTO.ProductoSimpleDTO(
                                    detalle.getProducto().getId(),
                                    detalle.getProducto().getNombre(),
                                    detalle.getProducto().getCodigo(),
                                    categoria
                            );
                        }

                        return new TrasladoMovimientoDTO.TrasladoDetalleSimpleDTO(
                                detalle.getId(),
                                detalle.getCantidad(),
                                producto
                        );
                    })
                    .collect(Collectors.toList());
        }

        return new TrasladoMovimientoDTO(
                traslado.getId(),
                traslado.getFecha(),
                sedeOrigen,
                sedeDestino,
                trabajadorConfirmacion,
                traslado.getFechaConfirmacion(),
                detalles
        );
    }
}