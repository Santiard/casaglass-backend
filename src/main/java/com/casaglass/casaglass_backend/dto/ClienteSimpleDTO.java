package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Cliente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteSimpleDTO {
    
    private Long id;
    private String nit;
    private String nombre;
    private String direccion;
    private String telefono;
    private String ciudad;
    private String correo;
    private Boolean credito;
    
    // Constructor desde entidad
    public ClienteSimpleDTO(Cliente cliente) {
        this.id = cliente.getId();
        this.nit = cliente.getNit();
        this.nombre = cliente.getNombre();
        this.direccion = cliente.getDireccion();
        this.telefono = cliente.getTelefono();
        this.ciudad = cliente.getCiudad();
        this.correo = cliente.getCorreo();
        this.credito = cliente.getCredito();
    }
}