package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.service.ProductoVidrioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos-vidrio")
// CORS configurado globalmente en CorsConfig.java
public class ProductoVidrioController {

    private final ProductoVidrioService service;

    public ProductoVidrioController(ProductoVidrioService service) {
        this.service = service;
    }

    // Listado con filtros opcionales (sin paginación):
    // /api/productos-vidrio
    // /api/productos-vidrio?q=templado
    // /api/productos-vidrio?mm=6.0
    // /api/productos-vidrio?categoriaId=1
    @GetMapping
    public List<ProductoVidrio> listar(@RequestParam(required = false, name = "q") String query,
                                       @RequestParam(required = false) Double mm,
                                       @RequestParam(required = false) Long categoriaId) {
        if (query != null && !query.isBlank()) {
            return service.buscar(query);
        }
        if (categoriaId != null) {
            return service.listarPorCategoriaId(categoriaId);
        }
        if (mm != null) {
            return service.listarPorMm(mm);
        }
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoVidrio> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ProductoVidrio> obtenerPorCodigo(@PathVariable String codigo) {
        return service.obtenerPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProductoVidrio producto) {
        try {
            return ResponseEntity.ok(service.guardar(producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ProductoVidrio producto) {
        try {
            return ResponseEntity.ok(service.actualizar(id, producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
