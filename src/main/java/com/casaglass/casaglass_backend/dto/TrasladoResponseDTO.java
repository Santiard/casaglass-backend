package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.model.Traslado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para respuesta de traslado con detalles completos
 * Incluye toda la información del traslado incluyendo color del producto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrasladoResponseDTO {
    
    private Long id;
    private SedeSimpleDTO sedeOrigen;
    private SedeSimpleDTO sedeDestino;
    private LocalDate fecha;
    private TrabajadorSimpleDTO trabajadorConfirmacion;
    private LocalDate fechaConfirmacion;
    private List<TrasladoDetalleResponseDTO> detalles;
    
    /**
     * Constructor desde entidad Traslado
     */
    public TrasladoResponseDTO(Traslado traslado) {
        this.id = traslado.getId();
        this.fecha = traslado.getFecha();
        this.fechaConfirmacion = traslado.getFechaConfirmacion();
        
        // Mapear sede origen
        if (traslado.getSedeOrigen() != null) {
            this.sedeOrigen = new SedeSimpleDTO(
                traslado.getSedeOrigen().getId(),
                traslado.getSedeOrigen().getNombre()
            );
        }
        
        // Mapear sede destino
        if (traslado.getSedeDestino() != null) {
            this.sedeDestino = new SedeSimpleDTO(
                traslado.getSedeDestino().getId(),
                traslado.getSedeDestino().getNombre()
            );
        }
        
        // Mapear trabajador confirmación
        if (traslado.getTrabajadorConfirmacion() != null) {
            this.trabajadorConfirmacion = new TrabajadorSimpleDTO(
                traslado.getTrabajadorConfirmacion().getId(),
                traslado.getTrabajadorConfirmacion().getNombre()
            );
        }
        
        // Mapear detalles con productos incluyendo color
        if (traslado.getDetalles() != null) {
            this.detalles = traslado.getDetalles().stream()
                .map(TrasladoDetalleResponseDTO::new)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * DTO simplificado para Sede
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SedeSimpleDTO {
        private Long id;
        private String nombre;
    }
    
    /**
     * DTO simplificado para Trabajador
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrabajadorSimpleDTO {
        private Long id;
        private String nombre;
    }
}
