package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ProductoActualizarDTO;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
// CORS configurado globalmente en CorsConfig.java
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    // GET /api/productos
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long categoriaId,
                                   @RequestParam(required = false, name = "q") String query) {
        try {
            List<Producto> productos;
            if (query != null && !query.isBlank()) {
                productos = service.buscar(query);
            } else if (categoriaId != null) {
                productos = service.listarPorCategoriaId(categoriaId);
            } else {
                productos = service.listar();
            }
            
            System.out.println("=== DEBUG PRODUCTO CONTROLLER ===");
            System.out.println("Query: " + query);
            System.out.println("CategoriaId: " + categoriaId);
            System.out.println("Productos retornados: " + productos.size());
            System.out.println("================================");
            
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            System.err.println("ERROR en ProductoController.listar: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
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
    public ResponseEntity<?> crear(@RequestBody Producto producto) {
        try {
            System.out.println("🔍 DEBUG CONTROLADOR Producto: Recibiendo producto");
            System.out.println("   - Tipo de objeto recibido: " + producto.getClass().getName());
            System.out.println("   - Es instancia de ProductoVidrio: " + (producto instanceof com.casaglass.casaglass_backend.model.ProductoVidrio));
            
            // ✅ Si es ProductoVidrio, NO debe crearse aquí, debe usar el endpoint /api/productos-vidrio
            if (producto instanceof com.casaglass.casaglass_backend.model.ProductoVidrio) {
                System.err.println("⚠️ ADVERTENCIA: ProductoVidrio recibido en endpoint /api/productos");
                System.err.println("   Debería usar el endpoint /api/productos-vidrio");
                return ResponseEntity.badRequest().body("Los productos vidrio deben crearse en /api/productos-vidrio");
            }
            
            return ResponseEntity.ok(service.guardar(producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ProductoActualizarDTO productoDTO) {
        try {
            return ResponseEntity.ok(service.actualizar(id, productoDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 💰 ACTUALIZAR SOLO EL COSTO DE UN PRODUCTO
     * PUT /api/productos/{id}/costo
     * 
     * Body esperado:
     * {
     *   "costo": 15000.0
     * }
     */
    @PutMapping("/{id}/costo")
    public ResponseEntity<Producto> actualizarCostoProducto(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        try {
            Double nuevoCosto = request.get("costo");
            if (nuevoCosto == null || nuevoCosto < 0) {
                return ResponseEntity.badRequest().build();
            }
            
            Producto producto = service.actualizarCosto(id, nuevoCosto);
            return ResponseEntity.ok(producto);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorias-texto")
    public List<String> categoriasTexto() {
        return service.listarCategoriasTexto();
    }
}
