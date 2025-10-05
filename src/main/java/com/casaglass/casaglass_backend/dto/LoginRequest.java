// src/main/java/com/casaglass/casaglass_backend/dto/LoginRequest.java
package com.casaglass.casaglass_backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
  private String username;
  private String password;
}
