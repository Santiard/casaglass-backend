package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    // Listado general, con filtros opcionales sin paginación
    // /api/productos
    // /api/productos?categoria=Vidrio
    // /api/productos?q=VID-001
    @GetMapping
    public List<Producto> listar(@RequestParam(required = false) String categoria,
                                 @RequestParam(required = false, name = "q") String query) {
        if (query != null && !query.isBlank()) {
            return service.buscar(query);
        }
        if (categoria != null && !categoria.isBlank()) {
            return service.listarPorCategoria(categoria);
        }
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Producto> obtenerPorCodigo(@PathVariable String codigo) {
        return service.obtenerPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Producto crear(@RequestBody Producto producto) {
        return service.guardar(producto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @RequestBody Producto producto) {
        try {
            return ResponseEntity.ok(service.actualizar(id, producto));
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
