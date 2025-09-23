package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.service.ProductoVidrioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos-vidrio")
@CrossOrigin(origins = "*")
public class ProductoVidrioController {

    private final ProductoVidrioService service;

    public ProductoVidrioController(ProductoVidrioService service) {
        this.service = service;
    }

    // Listado con filtros opcionales (sin paginación):
    // /api/productos-vidrio
    // /api/productos-vidrio?q=templado
    // /api/productos-vidrio?mm=6.0
    // /api/productos-vidrio?laminas=2
    @GetMapping
    public List<ProductoVidrio> listar(@RequestParam(required = false, name = "q") String query,
                                       @RequestParam(required = false) Double mm,
                                       @RequestParam(required = false) Integer laminas) {
        if (query != null && !query.isBlank()) {
            return service.buscar(query);
        }
        if (mm != null) {
            return service.listarPorMm(mm);
        }
        if (laminas != null) {
            return service.listarPorLaminas(laminas);
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
    public ProductoVidrio crear(@RequestBody ProductoVidrio producto) {
        return service.guardar(producto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoVidrio> actualizar(@PathVariable Long id, @RequestBody ProductoVidrio producto) {
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
