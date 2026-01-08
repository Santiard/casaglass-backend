package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO optimizado para listar movimientos de traslado en el frontend
 * Incluye información consolidada de sedes, trabajador y productos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrasladoMovimientoDTO {

    private Long id;
    private LocalDate fecha;
    private SedeSimpleDTO sedeOrigen;
    private SedeSimpleDTO sedeDestino;
    private TrabajadorSimpleDTO trabajadorConfirmacion;
    private LocalDate fechaConfirmacion;
    private List<TrasladoDetalleSimpleDTO> detalles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SedeSimpleDTO {
        private Long id;
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrabajadorSimpleDTO {
        private Long id;
        private String nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrasladoDetalleSimpleDTO {
        private Long id;
        private Double cantidad;
        private ProductoSimpleDTO producto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoSimpleDTO {
        private Long id;
        private String nombre;
        private String codigo;
        private String categoria;
    }
}