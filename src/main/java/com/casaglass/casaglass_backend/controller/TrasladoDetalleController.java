package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.TrasladoDetalleBatchDTO;
import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import com.casaglass.casaglass_backend.service.TrasladoDetalleService;
import com.casaglass.casaglass_backend.service.TrasladoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traslados/{trasladoId}/detalles")
// CORS configurado globalmente en CorsConfig.java
public class TrasladoDetalleController {

    private final TrasladoDetalleService service;
    private final TrasladoService trasladoService;

    public TrasladoDetalleController(TrasladoDetalleService service, TrasladoService trasladoService) {
        this.service = service;
        this.trasladoService = trasladoService;
    }

    @GetMapping
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
            return ResponseEntity.ok(service.crear(trasladoId, detalle));
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
            service.eliminar(trasladoId, detalleId);
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
    public ResponseEntity<?> actualizarBatch(@PathVariable Long trasladoId,
                                             @Valid @RequestBody TrasladoDetalleBatchDTO batchDTO) {
        try {
            System.out.println("📥 DEBUG Controller: Recibido batchDTO para trasladoId=" + trasladoId);
            System.out.println("📥 DEBUG Controller: batchDTO.getEliminar() = " + batchDTO.getEliminar());
            System.out.println("📥 DEBUG Controller: batchDTO.getCrear() = " + batchDTO.getCrear());
            System.out.println("📥 DEBUG Controller: batchDTO.getActualizar() = " + batchDTO.getActualizar());
            
            List<TrasladoDetalle> detallesActualizados = trasladoService.actualizarDetallesBatch(trasladoId, batchDTO);
            
            System.out.println("✅ DEBUG Controller: Proceso batch completado. Detalles retornados: " + detallesActualizados.size());
            return ResponseEntity.ok(detallesActualizados);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ ERROR Controller (IllegalArgumentException): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("❌ ERROR Controller (RuntimeException): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
