package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.TrasladoDetalleBatchDTO;
import com.casaglass.casaglass_backend.exception.InventarioInsuficienteException;
import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import com.casaglass.casaglass_backend.service.TrasladoDetalleService;
import com.casaglass.casaglass_backend.service.TrasladoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traslados/{trasladoId}/detalles")
@Tag(name = "Traslados — detalles", description = "Líneas de producto/corte. POST/PUT usan cuerpo TrasladoDetalle; batch usa productoInventarioADescontarSede1Id en crear[].")
// CORS configurado globalmente en CorsConfig.java
public class TrasladoDetalleController {

    private final TrasladoDetalleService service;
    private final TrasladoService trasladoService;

    public TrasladoDetalleController(TrasladoDetalleService service, TrasladoService trasladoService) {
        this.service = service;
        this.trasladoService = trasladoService;
    }

    @GetMapping
    @Operation(summary = "Listar detalles del traslado", description = "Incluye producto y productoInventarioADescontarSede1 (id anidado en objeto) cuando exista en BD.")
    public List<TrasladoDetalle> listar(@PathVariable Long trasladoId) {
        return service.listar(trasladoId);
    }

    @GetMapping("/{detalleId}")
public ResponseEntity<?> obtener(@PathVariable Long trasladoId,
                                 @PathVariable Long detalleId) {
    return service.obtener(detalleId)
            .map(detalle -> {
                if (!detalle.getTraslado().getId().equals(trasladoId)) {
                    return ResponseEntity.badRequest().body("El detalle no pertenece al traslado indicado");
                }
                return ResponseEntity.ok(detalle); // aquí sí va el detalle
            })
            .orElse(ResponseEntity.notFound().build());
}
    @PostMapping
    public ResponseEntity<?> agregar(@PathVariable Long trasladoId,
                                     @Valid @RequestBody TrasladoDetalle detalle) {
        try {
            return ResponseEntity.ok(trasladoService.agregarDetalle(trasladoId, detalle));
        } catch (InventarioInsuficienteException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{detalleId}")
    public ResponseEntity<?> actualizar(@PathVariable Long trasladoId,
                                        @PathVariable Long detalleId,
                                        @Valid @RequestBody TrasladoDetalle detalle) {
        try {
            // ✅ Usar TrasladoService.actualizarDetalle() que SÍ actualiza el inventario correctamente
            return ResponseEntity.ok(trasladoService.actualizarDetalle(trasladoId, detalleId, detalle));
        } catch (InventarioInsuficienteException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{detalleId}")
    public ResponseEntity<?> eliminar(@PathVariable Long trasladoId,
                                      @PathVariable Long detalleId) {
        try {
            trasladoService.eliminarDetalle(trasladoId, detalleId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 🔄 ACTUALIZAR MÚLTIPLES DETALLES EN BATCH (ATÓMICO)
     * Permite crear, actualizar y eliminar detalles en una sola transacción.
     * Esto evita problemas de concurrencia cuando se hacen múltiples cambios simultáneos.
     * 
     * Ejemplo de body:
     * {
     *   "crear": [
     *     { "productoId": 1, "cantidad": 10 }
     *   ],
     *   "actualizar": [
     *     { "detalleId": 5, "cantidad": 15 }
     *   ],
     *   "eliminar": [3, 4]
     * }
     */
    @PutMapping("/batch")
    @Operation(summary = "Batch atómico de detalles", description = "Ver TrasladoDetalleBatchDTO. Errores de validación 400; stock 409 (INVENTARIO_INSUFICIENTE). Tras fallo parcial, re-sincronizar con GET list antes de reintentar.")
    public ResponseEntity<?> actualizarBatch(@PathVariable Long trasladoId,
                                             @Valid @RequestBody TrasladoDetalleBatchDTO batchDTO) {
        try {
            List<TrasladoDetalle> detallesActualizados = trasladoService.actualizarDetallesBatch(trasladoId, batchDTO);
            return ResponseEntity.ok(detallesActualizados);
        } catch (InventarioInsuficienteException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
