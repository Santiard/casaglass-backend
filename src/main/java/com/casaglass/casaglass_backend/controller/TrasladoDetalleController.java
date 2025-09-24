package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.TrasladoDetalle;
import com.casaglass.casaglass_backend.service.TrasladoDetalleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traslados/{trasladoId}/detalles")
@CrossOrigin(origins = "*")
public class TrasladoDetalleController {

    private final TrasladoDetalleService service;

    public TrasladoDetalleController(TrasladoDetalleService service) {
        this.service = service;
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
            return ResponseEntity.ok(service.actualizar(trasladoId, detalleId, detalle));
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
}
