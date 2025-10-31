package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrabajadorResumenDTO {
    private Long id;
    private String username;
    private String nombre;
    private Rol rol;
}


