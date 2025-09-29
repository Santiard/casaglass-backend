package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Rol;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.service.TrabajadorService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trabajadores")
@CrossOrigin(origins = "*")
public class TrabajadorController {

    private final TrabajadorService service;

    public TrabajadorController(TrabajadorService service) {
        this.service = service;
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

    // Listar trabajadores por sede
    @GetMapping("/sede/{sedeId}")
    public List<Trabajador> listarPorSede(@PathVariable Long sedeId) {
        return service.listarPorSede(sedeId);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Trabajador trabajador) {
        try {
            return ResponseEntity.ok(service.crear(trabajador));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(e.getMessage()); // correo único
        }
    }

    @PutMapping("/{id}")
public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Trabajador t) {
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
}
