package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Trabajador;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrabajadorSimpleDTO {
    
    private Long id;
    private String nombre;
    private String correo;
    private String username;
    private String rol;
    private SedeSimpleDTO sede;
    
    // Constructor desde entidad
    public TrabajadorSimpleDTO(Trabajador trabajador) {
        this.id = trabajador.getId();
        this.nombre = trabajador.getNombre();
        this.correo = trabajador.getCorreo();
        this.username = trabajador.getUsername();
        this.rol = trabajador.getRol() != null ? trabajador.getRol().name() : null;
        this.sede = trabajador.getSede() != null ? new SedeSimpleDTO(trabajador.getSede()) : null;
    }
}