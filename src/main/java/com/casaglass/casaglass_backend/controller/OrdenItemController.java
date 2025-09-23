package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.service.OrdenItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes/{ordenId}/items")
@CrossOrigin(origins = "*")
public class OrdenItemController {

    private final OrdenItemService service;

    public OrdenItemController(OrdenItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrdenItem> listar(@PathVariable Long ordenId) {
        return service.listarPorOrden(ordenId);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<OrdenItem> obtener(@PathVariable Long ordenId, @PathVariable Long itemId) {
        return service.obtener(itemId)
                .filter(i -> i.getOrden().getId().equals(ordenId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@PathVariable Long ordenId, @Valid @RequestBody OrdenItem item) {
        try {
            return ResponseEntity.ok(service.crear(ordenId, item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> actualizar(@PathVariable Long ordenId,
                                        @PathVariable Long itemId,
                                        @Valid @RequestBody OrdenItem item) {
        try {
            return ResponseEntity.ok(service.actualizar(ordenId, itemId, item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> eliminar(@PathVariable Long ordenId, @PathVariable Long itemId) {
        try {
            service.eliminar(ordenId, itemId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
