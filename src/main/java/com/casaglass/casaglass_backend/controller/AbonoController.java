package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.service.AbonoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class AbonoController {

    private final AbonoService service;

    public AbonoController(AbonoService service) {
        this.service = service;
    }

    /* --------- Rutas anidadas bajo el crédito --------- */

    @GetMapping("/api/creditos/{creditoId}/abonos")
    public List<Abono> listarPorCredito(@PathVariable Long creditoId) {
        return service.listarPorCredito(creditoId);
    }

    @PostMapping("/api/creditos/{creditoId}/abonos")
    public ResponseEntity<?> crear(@PathVariable Long creditoId, @Valid @RequestBody Abono abono) {
        try {
            return ResponseEntity.ok(service.crear(creditoId, abono));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/api/creditos/{creditoId}/abonos/{abonoId}")
    public ResponseEntity<?> actualizar(@PathVariable Long creditoId,
                                        @PathVariable Long abonoId,
                                        @Valid @RequestBody Abono abono) {
        try {
            return ResponseEntity.ok(service.actualizar(creditoId, abonoId, abono));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/api/creditos/{creditoId}/abonos/{abonoId}")
    public ResponseEntity<?> eliminar(@PathVariable Long creditoId, @PathVariable Long abonoId) {
        try {
            service.eliminar(creditoId, abonoId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /* ----------------- Otras consultas útiles ----------------- */

    @GetMapping("/api/abonos/{abonoId}")
    public ResponseEntity<Abono> obtener(@PathVariable Long abonoId) {
        return service.obtener(abonoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/abonos/cliente/{clienteId}")
    public List<Abono> listarPorCliente(@PathVariable Long clienteId) {
        return service.listarPorCliente(clienteId);
    }

    @GetMapping("/api/abonos/orden/{ordenId}")
    public List<Abono> listarPorOrden(@PathVariable Long ordenId) {
        return service.listarPorOrden(ordenId);
    }
}
