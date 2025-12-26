package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.service.CorteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cortes")
// CORS configurado globalmente en CorsConfig.java
public class CorteController {

    private final CorteService service;

    public CorteController(CorteService service) {
        this.service = service;
    }

    // Listado general con filtros opcionales
    @GetMapping
    public List<Corte> listar(@RequestParam(required = false) String categoria,
                              @RequestParam(required = false) Long categoriaId,
                              @RequestParam(required = false, name = "q") String query,
                              @RequestParam(required = false) Double largoMin,
                              @RequestParam(required = false) Double largoMax,
                              @RequestParam(required = false) Double precioMin,
                              @RequestParam(required = false) Double precioMax,
                              @RequestParam(required = false) Double largoMinimo,
                              @RequestParam(required = false) Double precioMaximo,
                              @RequestParam(required = false, defaultValue = "false") boolean conObservaciones,
                              @RequestParam(required = false) String tipo,
                              @RequestParam(required = false) String color) {
        
        // 🆕 NUEVOS FILTROS POR TIPO Y COLOR
        if (tipo != null && !tipo.isBlank()) {
            if (categoriaId != null) {
                return service.listarPorCategoriaYTipo(categoriaId, tipo);
            }
            return service.listarPorTipo(tipo);
        }
        
        if (color != null && !color.isBlank()) {
            if (categoriaId != null) {
                return service.listarPorCategoriaYColor(categoriaId, color);
            }
            return service.listarPorColor(color);
        }
        
        // Filtros específicos existentes
        if (conObservaciones) {
            return service.listarConObservaciones();
        }
        
        if (largoMin != null && largoMax != null) {
            return service.listarPorRangoLargo(largoMin, largoMax);
        }
        
        if (precioMin != null && precioMax != null) {
            return service.listarPorRangoPrecio(precioMin, precioMax);
        }
        
        if (largoMinimo != null) {
            return service.listarPorLargoMinimo(largoMinimo);
        }
        
        if (precioMaximo != null) {
            return service.listarPorPrecioMaximo(precioMaximo);
        }
        
        if (query != null && !query.isBlank()) {
            return service.buscar(query);
        }
        
        // 🔁 Filtro por ID de categoría (recomendado)
        if (categoriaId != null) {
            return service.listarPorCategoriaId(categoriaId);
        }
        
        // 🔁 Mantener compatibilidad con filtro por nombre de categoría
        if (categoria != null && !categoria.isBlank()) {
            return service.listarPorCategoria(categoria);
        }
        
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Corte> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Corte> obtenerPorCodigo(@PathVariable String codigo) {
        return service.obtenerPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Corte corte) {
        try {
            Corte resultado = service.guardar(corte);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Corte corte) {
        try {
            Corte resultado = service.actualizar(id, corte);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar: " + e.getMessage());
        }
    }

    // Endpoints específicos para consultas especializadas
    @GetMapping("/categoria/{categoria}")
    public List<Corte> listarPorCategoria(@PathVariable String categoria) {
        return service.listarPorCategoria(categoria);
    }

    @GetMapping("/buscar")
    public List<Corte> buscar(@RequestParam String q) {
        return service.buscar(q);
    }

    @GetMapping("/largo")
    public List<Corte> listarPorRangoLargo(@RequestParam Double min, @RequestParam Double max) {
        return service.listarPorRangoLargo(min, max);
    }

    @GetMapping("/precio")
    public List<Corte> listarPorRangoPrecio(@RequestParam Double min, @RequestParam Double max) {
        return service.listarPorRangoPrecio(min, max);
    }

    @GetMapping("/observaciones")
    public List<Corte> listarConObservaciones() {
        return service.listarConObservaciones();
    }

    // 🆕 NUEVOS ENDPOINTS ESPECÍFICOS
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Corte>> listarPorTipo(@PathVariable String tipo) {
        try {
            return ResponseEntity.ok(service.listarPorTipo(tipo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/color/{color}")
    public ResponseEntity<List<Corte>> listarPorColor(@PathVariable String color) {
        try {
            return ResponseEntity.ok(service.listarPorColor(color));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/categoria/{categoriaId}/tipo/{tipo}")
    public ResponseEntity<List<Corte>> listarPorCategoriaYTipo(
            @PathVariable Long categoriaId, 
            @PathVariable String tipo) {
        try {
            return ResponseEntity.ok(service.listarPorCategoriaYTipo(categoriaId, tipo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/categoria/{categoriaId}/color/{color}")
    public ResponseEntity<List<Corte>> listarPorCategoriaYColor(
            @PathVariable Long categoriaId, 
            @PathVariable String color) {
        try {
            return ResponseEntity.ok(service.listarPorCategoriaYColor(categoriaId, color));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/buscar-por-ids")
    public ResponseEntity<List<Corte>> listarPorIds(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(service.listarPorIds(ids));
    }
}