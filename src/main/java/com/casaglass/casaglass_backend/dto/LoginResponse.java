package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
  private Long id;
  private String nombre;
  private String correo;
  private String username;
  private String rol;
  private Long sedeId;
  private String sedeNombre;
}
