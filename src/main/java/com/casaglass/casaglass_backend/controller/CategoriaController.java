package com.casaglass.casaglass_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaglass.casaglass_backend.model.Categoria;
import com.casaglass.casaglass_backend.service.CategoriaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
// CORS configurado globalmente en CorsConfig.java
public class CategoriaController {

    private final CategoriaService categoriaService;

    // GET /api/categorias
    @GetMapping
    public ResponseEntity<List<Categoria>> listar() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    // GET /api/categorias/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/categorias
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Categoria categoria) {
        try {
            Categoria creada = categoriaService.crear(categoria);
            return ResponseEntity.ok(creada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/categorias/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Categoria categoria) {
        try {
            Categoria actualizada = categoriaService.actualizar(id, categoria);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/categorias/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            categoriaService.eliminar(id);
            return ResponseEntity.ok().body("Categoría eliminada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
