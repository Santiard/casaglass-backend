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
     * 🚀 LISTADO DE TRASLADOS CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object obtenerMovimientosConFiltros(
            Long sedeOrigenId,
            Long sedeDestinoId,
            Long sedeId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            Boolean confirmado,
            Long trabajadorId,
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder) {
        
        // Validar fechas
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        
        // Convertir estado a confirmado si se proporciona
        // estado: "PENDIENTE" -> confirmado = false, "CONFIRMADO" -> confirmado = true
        if (estado != null && !estado.isEmpty()) {
            String estadoUpper = estado.toUpperCase();
            if ("PENDIENTE".equals(estadoUpper)) {
                confirmado = false;
            } else if ("CONFIRMADO".equals(estadoUpper)) {
                confirmado = true;
            } else if (!"CANCELADO".equals(estadoUpper)) {
                throw new IllegalArgumentException("Estado inválido: " + estado + ". Valores válidos: PENDIENTE, CONFIRMADO");
            }
            // CANCELADO no se maneja actualmente en el modelo
        }
        
        // Validar y normalizar ordenamiento
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "fecha";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "DESC";
        }
        sortOrder = sortOrder.toUpperCase();
        if (!sortOrder.equals("ASC") && !sortOrder.equals("DESC")) {
            sortOrder = "DESC";
        }
        
        // Buscar traslados con filtros
        List<Traslado> traslados = trasladoRepository.buscarConFiltros(
            sedeOrigenId, sedeDestinoId, sedeId, fechaDesde, fechaHasta, confirmado, trabajadorId
        );
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por fecha DESC)
        if (!sortBy.equals("fecha") || !sortOrder.equals("DESC")) {
            traslados = aplicarOrdenamientoTraslados(traslados, sortBy, sortOrder);
        }
        
        // Convertir a DTOs
        List<TrasladoMovimientoDTO> dtos = traslados.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; // Límite máximo
            
            long totalElements = dtos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, dtos.size());
            
            if (fromIndex >= dtos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<TrasladoMovimientoDTO> contenido = dtos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return dtos;
    }
    
    /**
     * Aplica ordenamiento a la lista de traslados según sortBy y sortOrder
     */
    private List<Traslado> aplicarOrdenamientoTraslados(List<Traslado> traslados, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "fecha":
                traslados.sort((a, b) -> {
                    int cmp = a.getFecha().compareTo(b.getFecha());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "id":
                traslados.sort((a, b) -> {
                    int cmp = Long.compare(a.getId(), b.getId());
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por fecha DESC
                traslados.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        }
        
        return traslados;
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