package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.LoginRequest;
import com.casaglass.casaglass_backend.dto.LoginResponse;
import com.casaglass.casaglass_backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.login(request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body("Usuario o contraseña inválidos"));
    }
}

