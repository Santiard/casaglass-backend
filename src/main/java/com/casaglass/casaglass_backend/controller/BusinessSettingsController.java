package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.BusinessSettings;
import com.casaglass.casaglass_backend.service.BusinessSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business-settings")
// CORS configurado globalmente en CorsConfig.java
public class BusinessSettingsController {

    private final BusinessSettingsService service;

    public BusinessSettingsController(BusinessSettingsService service) {
        this.service = service;
    }

    /**
     * 📋 OBTENER CONFIGURACIÓN ACTUAL
     * GET /api/business-settings
     * 
     * Retorna la configuración actual de negocio (IVA, retención, umbrales)
     * Si no existe configuración, retorna valores por defecto
     * 
     * Respuesta:
     * {
     *   "id": 1,
     *   "ivaRate": 19.0,
     *   "reteRate": 2.5,
     *   "reteThreshold": 1000000,
     *   "updatedAt": "2025-01-15"
     * }
     */
    @GetMapping
    public ResponseEntity<BusinessSettings> obtenerConfiguracion() {
        BusinessSettings config = service.obtenerConfiguracion();
        return ResponseEntity.ok(config);
    }

    /**
     * 📋 OBTENER CONFIGURACIÓN POR ID
     * GET /api/business-settings/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessSettings> obtenerPorId(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 📋 LISTAR TODAS LAS CONFIGURACIONES
     * GET /api/business-settings/all
     * 
     * Normalmente solo debería haber una configuración
     */
    @GetMapping("/all")
    public ResponseEntity<List<BusinessSettings>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /**
     * 💾 CREAR CONFIGURACIÓN
     * POST /api/business-settings
     * 
     * Body esperado:
     * {
     *   "ivaRate": 19.0,        // Porcentaje de IVA (0-100)
     *   "reteRate": 2.5,        // Porcentaje de retención (0-100)
     *   "reteThreshold": 1000000  // Umbral mínimo en COP
     * }
     * 
     * Nota: updatedAt se establece automáticamente
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody BusinessSettings settings) {
        try {
            BusinessSettings creada = service.crear(settings);
            return ResponseEntity.ok(creada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al crear configuración: " + e.getMessage()));
        }
    }

    /**
     * 🔄 ACTUALIZAR CONFIGURACIÓN POR ID
     * PUT /api/business-settings/{id}
     * 
     * Body esperado:
     * {
     *   "ivaRate": 19.0,
     *   "reteRate": 2.5,
     *   "reteThreshold": 1000000
     * }
     * 
     * Nota: updatedAt se actualiza automáticamente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody BusinessSettings settings) {
        try {
            BusinessSettings actualizada = service.actualizar(id, settings);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al actualizar configuración: " + e.getMessage()));
        }
    }

    /**
     * 🔄 ACTUALIZAR CONFIGURACIÓN ACTUAL (sin ID)
     * PUT /api/business-settings
     * 
     * Actualiza la primera configuración encontrada o crea una nueva si no existe
     * 
     * Body esperado:
     * {
     *   "ivaRate": 19.0,
     *   "reteRate": 2.5,
     *   "reteThreshold": 1000000
     * }
     * 
     * Este es el endpoint más útil para el frontend, ya que normalmente solo hay una configuración
     */
    @PutMapping
    public ResponseEntity<?> actualizarConfiguracion(@RequestBody BusinessSettings settings) {
        try {
            BusinessSettings actualizada = service.actualizarConfiguracion(settings);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al actualizar configuración: " + e.getMessage()));
        }
    }

    /**
     * 🗑️ ELIMINAR CONFIGURACIÓN
     * DELETE /api/business-settings/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al eliminar configuración: " + e.getMessage()));
        }
    }
}



