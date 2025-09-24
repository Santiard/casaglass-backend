package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Ingreso;
import com.casaglass.casaglass_backend.service.IngresoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
            @RequestParam LocalDateTime fechaInicio,
            @RequestParam LocalDateTime fechaFin) {
        return ingresoService.obtenerIngresosPorFecha(fechaInicio, fechaFin);
    }

    @PostMapping
    public ResponseEntity<Ingreso> crearIngreso(@RequestBody Ingreso ingreso) {
        try {
            Ingreso ingresoCreado = ingresoService.guardarIngreso(ingreso);
            return ResponseEntity.ok(ingresoCreado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ingreso> actualizarIngreso(
            @PathVariable Long id,
            @RequestBody Ingreso ingreso) {
        try {
            Ingreso ingresoActualizado = ingresoService.actualizarIngreso(id, ingreso);
            return ResponseEntity.ok(ingresoActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarIngreso(@PathVariable Long id) {
        try {
            ingresoService.eliminarIngreso(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/procesar")
    public ResponseEntity<String> procesarInventario(@PathVariable Long id) {
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
    public ResponseEntity<String> reprocesarInventario(@PathVariable Long id) {
        try {
            ingresoService.reprocesarInventario(id);
            return ResponseEntity.ok("Inventario reprocesado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}