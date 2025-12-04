// src/main/java/com/casaglass/casaglass_backend/controller/IngresoController.java
package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Ingreso;
import com.casaglass.casaglass_backend.service.IngresoService;
import com.casaglass.casaglass_backend.dto.IngresoCreateDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ingresos")
// CORS configurado globalmente en CorsConfig.java
public class IngresoController {

    private final IngresoService ingresoService;

    public IngresoController(IngresoService ingresoService) {
        this.ingresoService = ingresoService;
    }

    @GetMapping
    public List<Ingreso> listarIngresos(@RequestParam(required = false) Long sedeId) {
        if (sedeId != null) {
            return ingresoService.listarIngresosPorSede(sedeId);
        }
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
    public ResponseEntity<?> crearIngreso(@RequestBody IngresoCreateDTO ingresoDTO) {
        try {
            System.out.println("🔄 POST /api/ingresos - Creando ingreso desde DTO");
            System.out.println("📥 Datos recibidos - Proveedor ID: " + 
                (ingresoDTO.getProveedor() != null ? ingresoDTO.getProveedor().getId() : "null"));
            System.out.println("📥 Detalles: " + 
                (ingresoDTO.getDetalles() != null ? ingresoDTO.getDetalles().size() : 0) + " items");
            
            Ingreso resultado = ingresoService.crearIngresoDesdeDTO(ingresoDTO);
            
            System.out.println("✅ Ingreso creado exitosamente - ID: " + resultado.getId());
            
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("❌ Error de ejecución: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error inesperado: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarIngreso(@PathVariable Long id, @RequestBody Ingreso ingreso) {
        try {
            System.out.println("🔄 PUT /api/ingresos/" + id + " - Iniciando actualización");
            System.out.println("📥 Datos recibidos: " + ingreso.getNumeroFactura());
            
            Ingreso resultado = ingresoService.actualizarIngreso(id, ingreso);
            
            System.out.println("✅ Actualización exitosa - ID: " + resultado.getId());
            
            // 🔧 ARREGLO: Recargar la entidad para evitar problemas de serialización
            Ingreso ingresoLimpio = ingresoService.obtenerIngresoPorId(id)
                    .orElseThrow(() -> new RuntimeException("Error al recargar ingreso"));
            
            return ResponseEntity.ok(ingresoLimpio);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("❌ Error de ejecución: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error inesperado: " + e.getMessage());
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

    @PutMapping("/{id}/procesar")
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

    @PutMapping("/{id}/reprocesar")
    public ResponseEntity<?> reprocesarInventario(@PathVariable Long id) {
        try {
            ingresoService.reprocesarInventario(id);
            return ResponseEntity.ok("Inventario reprocesado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/marcar-procesado")
    public ResponseEntity<?> marcarComoProcesado(@PathVariable Long id) {
        try {
            Ingreso ingreso = ingresoService.marcarComoProcesado(id);
            return ResponseEntity.ok(ingreso);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
