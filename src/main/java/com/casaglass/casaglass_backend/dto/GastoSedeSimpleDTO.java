package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.GastoSede;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GastoSedeSimpleDTO {
    
    private Long id;
    private LocalDate fechaGasto;
    private Double monto;
    private String concepto;
    private String descripcion;
    private String comprobante;
    private String tipo;
    private Boolean aprobado;
    private String observaciones;
    
    // Información adicional sin entidades completas
    private String sedeNombre;
    private String empleadoNombre;
    private String proveedorNombre;
    
    // Constructor desde entidad (SIN referencias para evitar ciclos)
    public GastoSedeSimpleDTO(GastoSede gasto) {
        this.id = gasto.getId();
        this.fechaGasto = gasto.getFechaGasto();
        this.monto = gasto.getMonto();
        this.concepto = gasto.getConcepto();
        this.descripcion = gasto.getDescripcion();
        this.comprobante = gasto.getComprobante();
        this.tipo = gasto.getTipo().name();
        this.aprobado = gasto.getAprobado();
        this.observaciones = gasto.getObservaciones();
        
        // Solo nombres para evitar referencias completas
        this.sedeNombre = gasto.getSede() != null ? gasto.getSede().getNombre() : null;
        this.empleadoNombre = gasto.getEmpleado() != null ? gasto.getEmpleado().getNombre() : null;
        this.proveedorNombre = gasto.getProveedor() != null ? gasto.getProveedor().getNombre() : null;
    }
}