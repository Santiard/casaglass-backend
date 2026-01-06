package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.TrasladoResponseDTO;
import com.casaglass.casaglass_backend.model.Traslado;
import com.casaglass.casaglass_backend.service.TrasladoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/traslados")
// CORS configurado globalmente en CorsConfig.java
public class TrasladoController {

    private final TrasladoService service;

    public TrasladoController(TrasladoService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Traslado traslado) {
        try {
            return ResponseEntity.ok(service.crear(traslado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public List<Traslado> listar(@RequestParam(required = false) Long sedeOrigenId,
                                 @RequestParam(required = false) Long sedeDestinoId) {
        if (sedeOrigenId != null && sedeDestinoId != null) {
            // combinación simple
            return service.listarPorSedeOrigen(sedeOrigenId).stream()
                    .filter(t -> t.getSedeDestino().getId().equals(sedeDestinoId))
                    .toList();
        }
        if (sedeOrigenId != null) return service.listarPorSedeOrigen(sedeOrigenId);
        if (sedeDestinoId != null) return service.listarPorSedeDestino(sedeDestinoId);
        return service.listar();
    }

    @GetMapping("/fecha/{fecha}")
    public List<Traslado> listarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listarPorFecha(fecha);
    }

    @GetMapping("/fecha")
    public List<Traslado> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarPorRango(desde, hasta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrasladoResponseDTO> obtener(@PathVariable Long id) {
        return service.obtener(id)
                .map(traslado -> ResponseEntity.ok(new TrasladoResponseDTO(traslado)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCabecera(@PathVariable Long id, @RequestBody Traslado traslado) {
        try {
            return ResponseEntity.ok(service.actualizarCabecera(id, traslado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id, @RequestParam Long trabajadorId) {
        try {
            return ResponseEntity.ok(service.confirmarLlegada(id, trabajadorId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
