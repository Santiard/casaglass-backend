package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Banco;
import com.casaglass.casaglass_backend.service.BancoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🏦 CONTROLADOR DE BANCOS
 * Endpoints para gestionar los bancos disponibles como métodos de pago
 */
@RestController
@RequestMapping("/api/bancos")
// CORS configurado globalmente en CorsConfig.java
public class BancoController {

    private final BancoService bancoService;

    public BancoController(BancoService bancoService) {
        this.bancoService = bancoService;
    }

    /**
     * 📋 LISTAR TODOS LOS BANCOS
     * GET /api/bancos
     */
    @GetMapping
    public ResponseEntity<List<Banco>> listarTodos() {
        return ResponseEntity.ok(bancoService.listarTodos());
    }

    /**
     * 🔍 OBTENER BANCO POR ID
     * GET /api/bancos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Banco> obtenerPorId(@PathVariable Long id) {
        return bancoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ➕ CREAR NUEVO BANCO
     * POST /api/bancos
     * 
     * Body: { "nombre": "Bancolombia" }
     */
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody Banco banco) {
        try {
            Banco bancoCreado = bancoService.crear(banco);
            return ResponseEntity.status(HttpStatus.CREATED).body(bancoCreado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * ✏️ ACTUALIZAR BANCO
     * PUT /api/bancos/{id}
     * 
     * Body: { "nombre": "Bancolombia S.A." }
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody Banco banco) {
        try {
            Banco bancoActualizado = bancoService.actualizar(id, banco);
            return ResponseEntity.ok(bancoActualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🗑️ ELIMINAR BANCO
     * DELETE /api/bancos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            bancoService.eliminar(id);
            return ResponseEntity.ok().body("Banco eliminado correctamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
