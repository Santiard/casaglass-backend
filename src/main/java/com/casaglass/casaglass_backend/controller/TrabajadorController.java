package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.TrabajadorResumenDTO;
import com.casaglass.casaglass_backend.dto.TrabajadorDashboardDTO;
import com.casaglass.casaglass_backend.model.Rol;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.service.TrabajadorService;
import com.casaglass.casaglass_backend.service.TrabajadorDashboardService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trabajadores")
// CORS configurado globalmente en CorsConfig.java
public class TrabajadorController {

    private final TrabajadorService service;
    private final TrabajadorDashboardService dashboardService;

    public TrabajadorController(TrabajadorService service, TrabajadorDashboardService dashboardService) {
        this.service = service;
        this.dashboardService = dashboardService;
    }

    // Listado general o con filtros:
    // /api/trabajadores
    // /api/trabajadores?q=texto      (busca en nombre O correo)
    // /api/trabajadores?rol=VENDEDOR
    // /api/trabajadores?sedeId=1
    // /api/trabajadores?rol=VENDEDOR&sedeId=1
    @GetMapping
    public List<Trabajador> listar(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) Rol rol,
                                   @RequestParam(required = false) Long sedeId) {
        if (q != null && !q.isBlank()) return service.buscar(q.trim());
        if (rol != null && sedeId != null) return service.listarPorRolYSede(rol, sedeId);
        if (rol != null) return service.listarPorRol(rol);
        if (sedeId != null) return service.listarPorSede(sedeId);
        return service.listar();
    }

    // 🚀 Listado resumido para tabla: id, username, nombre, rol
    @GetMapping("/tabla")
    public List<TrabajadorResumenDTO> listarResumen(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Rol rol,
                                                    @RequestParam(required = false) Long sedeId) {
        List<Trabajador> base = listar(q, rol, sedeId);
        return base.stream()
                .map(t -> new TrabajadorResumenDTO(t.getId(), t.getUsername(), t.getNombre(), t.getRol()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trabajador> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Búsqueda exacta por correo (case-insensitive)
    @GetMapping("/correo/{correo}")
    public ResponseEntity<Trabajador> obtenerPorCorreo(@PathVariable String correo) {
        return service.obtenerPorCorreo(correo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Búsqueda por texto en nombre, correo o username
    @GetMapping("/buscar")
    public List<Trabajador> buscarPorTexto(@RequestParam String q) {
        return service.buscarPorTexto(q);
    }

    // Búsqueda exacta por username
    @GetMapping("/username/{username}")
    public ResponseEntity<Trabajador> obtenerPorUsername(@PathVariable String username) {
        return service.obtenerPorUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Listar trabajadores por sede
    @GetMapping("/sede/{sedeId}")
    public List<Trabajador> listarPorSede(@PathVariable Long sedeId) {
        return service.listarPorSede(sedeId);
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody Trabajador trabajador) {
        try {
            return ResponseEntity.ok(service.crear(trabajador));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(e.getMessage()); // correo único
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody Trabajador t) {
  try {
    return ResponseEntity.ok(service.actualizar(id, t));
  } catch (org.springframework.dao.DataIntegrityViolationException e) {
    return ResponseEntity.status(409).body("Violación de integridad: " + e.getMostSpecificCause().getMessage());
  } catch (IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  } catch (RuntimeException e) { // después de los específicos
    return ResponseEntity.status(404).body(e.getMessage());
  } catch (Exception e) {        // último catch genérico
    return ResponseEntity.status(500).body("Error inesperado");
  }
}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // 🔐 Cambiar contraseña
    @PutMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuevaPassword = body != null ? body.get("password") : null;
        try {
            Trabajador actualizado = service.cambiarPassword(id, nuevaPassword);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Contraseña actualizada",
                    "id", actualizado.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error inesperado"));
        }
    }

    // 📊 Dashboard del trabajador
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<TrabajadorDashboardDTO> obtenerDashboardTrabajador(
            @PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta
    ) {
        try {
            return ResponseEntity.ok(dashboardService.obtenerDashboard(id, desde, hasta));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
