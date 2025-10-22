package com.casaglass.casaglass_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngresoCreateDTO {
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;
    
    private ProveedorIdDTO proveedor;
    private String numeroFactura;
    private String observaciones;
    private List<IngresoDetalleCreateDTO> detalles;
    private Double totalCosto;
    private Boolean procesado;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProveedorIdDTO {
        private Long id;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngresoDetalleCreateDTO {
        private ProductoIdDTO producto;
        private Integer cantidad;
        private Double costoUnitario;
        private Double totalLinea;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductoIdDTO {
            private Long id;
        }
    }
}
