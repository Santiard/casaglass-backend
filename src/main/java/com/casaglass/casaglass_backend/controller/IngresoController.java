// src/main/java/com/casaglass/casaglass_backend/controller/IngresoController.java
package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Ingreso;
import com.casaglass.casaglass_backend.service.IngresoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ingresos")
@CrossOrigin(origins = "*")
public class IngresoController {

    private final IngresoService ingresoService;

    public IngresoController(IngresoService ingresoService) {
        this.ingresoService = ingresoService;
    }

    @GetMapping
    public List<Ingreso> listarIngresos() {
        return ingresoService.listarIngresos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ingreso> obtenerIngreso(@PathVariable Long id) {
        // IMPORTANTE: no mezclar tipos en las ramas (no pongas body("...") aquí)
        return ingresoService.obtenerIngresoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/proveedor/{proveedorId}")
    public List<Ingreso> obtenerIngresosPorProveedor(@PathVariable Long proveedorId) {
        return ingresoService.obtenerIngresosPorProveedor(proveedorId);
    }

    @GetMapping("/no-procesados")
    public List<Ingreso> obtenerIngresosNoProcesados() {
        return ingresoService.obtenerIngresosNoProcesados();
    }

    @GetMapping("/por-fecha")
    public List<Ingreso> obtenerIngresosPorFecha(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin) {
        return ingresoService.obtenerIngresosPorFecha(fechaInicio, fechaFin);
    }

    @PostMapping
    public ResponseEntity<?> crearIngreso(@RequestBody Ingreso ingreso) {
        // Aquí sí retornamos mensajes (ResponseEntity<?>), y el handler global formatea 400/500
        try {
            return ResponseEntity.ok(ingresoService.guardarIngreso(ingreso));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarIngreso(@PathVariable Long id, @RequestBody Ingreso ingreso) {
        try {
            return ResponseEntity.ok(ingresoService.actualizarIngreso(id, ingreso));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarIngreso(@PathVariable Long id) {
        try {
            ingresoService.eliminarIngreso(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/procesar")
    public ResponseEntity<?> procesarInventario(@PathVariable Long id) {
        try {
            Ingreso ingreso = ingresoService.obtenerIngresoPorId(id)
                    .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));
            ingresoService.procesarInventario(ingreso);
            return ResponseEntity.ok("Inventario procesado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reprocesar")
    public ResponseEntity<?> reprocesarInventario(@PathVariable Long id) {
        try {
            ingresoService.reprocesarInventario(id);
            return ResponseEntity.ok("Inventario reprocesado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
