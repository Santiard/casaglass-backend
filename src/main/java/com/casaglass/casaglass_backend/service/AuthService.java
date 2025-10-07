package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.LoginRequest;
import com.casaglass.casaglass_backend.dto.LoginResponse;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final TrabajadorRepository repo;

    public AuthService(TrabajadorRepository repo) {
        this.repo = repo;
    }

    public Optional<LoginResponse> login(LoginRequest req) {
        return repo.findByUsername(req.getUsername())
                .filter(t -> t.getPassword().equals(req.getPassword()))
                .map(this::toResponse);
    }

    private LoginResponse toResponse(Trabajador t) {
        return new LoginResponse(
                t.getId(),
                t.getNombre(),
                t.getCorreo(),
                t.getUsername(),
                t.getRol().name(),
                t.getSede() != null ? t.getSede().getId() : null,
                t.getSede() != null ? t.getSede().getNombre() : null
        );
    }
}
