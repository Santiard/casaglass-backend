package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Sede;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SedeSimpleDTO {
    
    private Long id;
    private String nombre;
    private String direccion;
    private String ciudad;
    
    // Constructor desde entidad
    public SedeSimpleDTO(Sede sede) {
        this.id = sede.getId();
        this.nombre = sede.getNombre();
        this.direccion = sede.getDireccion();
        this.ciudad = sede.getCiudad();
    }
}