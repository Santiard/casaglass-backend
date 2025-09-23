package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.service.CreditoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creditos")
@CrossOrigin(origins = "*")
public class CreditoController {

    private final CreditoService service;

    public CreditoController(CreditoService service) {
        this.service = service;
    }

    /** Crear crédito para un cliente (body: { "cliente": { "id": X } }) */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Credito credito) {
        try {
            return ResponseEntity.ok(service.crear(credito));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Obtener todos los créditos */
    @GetMapping
    public List<Credito> listar() {
        return service.listar();
    }

    /** Obtener crédito por id */
    @GetMapping("/{id}")
    public ResponseEntity<Credito> obtener(@PathVariable Long id) {
        return service.obtener(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** Obtener crédito por cliente (si sólo manejas uno por cliente) */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<Credito> obtenerPorCliente(@PathVariable Long clienteId) {
        return service.obtenerPorCliente(clienteId).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Listar créditos por cliente (si permites varios por cliente) */
    @GetMapping("/cliente/{clienteId}/todos")
    public List<Credito> listarPorCliente(@PathVariable Long clienteId) {
        return service.listarPorCliente(clienteId);
    }

    /** Agregar órdenes al crédito (body: [1,2,3]) */
    @PostMapping("/{creditoId}/ordenes")
    public ResponseEntity<?> agregarOrdenes(@PathVariable Long creditoId, @RequestBody List<Long> ordenIds) {
        try {
            return ResponseEntity.ok(service.agregarOrdenes(creditoId, ordenIds));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Quitar una orden del crédito */
    @DeleteMapping("/{creditoId}/ordenes/{ordenId}")
    public ResponseEntity<?> quitarOrden(@PathVariable Long creditoId, @PathVariable Long ordenId) {
        try {
            return ResponseEntity.ok(service.quitarOrden(creditoId, ordenId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Recalcular totalDeuda (sumando órdenes y restando abonos) */
    @PostMapping("/{creditoId}/recalcular")
    public ResponseEntity<?> recalcular(@PathVariable Long creditoId) {
        try {
            return ResponseEntity.ok(service.recalcular(creditoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Eliminar crédito */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}