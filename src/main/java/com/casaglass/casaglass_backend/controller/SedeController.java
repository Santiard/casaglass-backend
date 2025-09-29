package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.service.SedeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sedes")
@CrossOrigin(origins = "*")
public class SedeController {

    private final SedeService service;

    public SedeController(SedeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Sede> listar(@RequestParam(required = false) String q,
                             @RequestParam(required = false) String ciudad) {
        if (q != null && !q.isBlank()) return service.buscar(q.trim());
        if (ciudad != null && !ciudad.isBlank()) return service.buscarPorCiudad(ciudad.trim());
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sede> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<Sede> obtenerPorNombre(@PathVariable String nombre) {
        return service.obtenerPorNombre(nombre).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint para obtener todos los trabajadores de una sede
    @GetMapping("/{id}/trabajadores")
    public ResponseEntity<List<Trabajador>> obtenerTrabajadoresDeSede(@PathVariable Long id) {
        try {
            List<Trabajador> trabajadores = service.obtenerTrabajadoresDeSede(id);
            return ResponseEntity.ok(trabajadores);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Sede sede) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(sede));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Violación de integridad (¿nombre duplicado?): " + e.getMostSpecificCause().getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Sede sede) {
        try {
            return ResponseEntity.ok(service.actualizar(id, sede));
        } catch (DataIntegrityViolationException e) { // <-- primero el específico
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Violación de integridad: " + e.getMostSpecificCause().getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { // <-- luego el genérico
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se puede eliminar: está referenciada por otros registros.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado");
        }
    }
}
