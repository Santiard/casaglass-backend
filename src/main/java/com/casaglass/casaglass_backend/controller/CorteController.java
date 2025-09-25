package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.service.CorteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cortes")
@CrossOrigin(origins = "*")
public class CorteController {

    private final CorteService service;

    public CorteController(CorteService service) {
        this.service = service;
    }

    // Listado general con filtros opcionales
    @GetMapping
    public List<Corte> listar(@RequestParam(required = false) String categoria,
                              @RequestParam(required = false, name = "q") String query,
                              @RequestParam(required = false) BigDecimal largoMin,
                              @RequestParam(required = false) BigDecimal largoMax,
                              @RequestParam(required = false) BigDecimal precioMin,
                              @RequestParam(required = false) BigDecimal precioMax,
                              @RequestParam(required = false) BigDecimal largoMinimo,
                              @RequestParam(required = false) BigDecimal precioMaximo,
                              @RequestParam(required = false, defaultValue = "false") boolean conObservaciones) {
        
        // Filtros específicos
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
    public ResponseEntity<Corte> crear(@RequestBody Corte corte) {
        try {
            return ResponseEntity.ok(service.guardar(corte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Corte> actualizar(@PathVariable Long id, @RequestBody Corte corte) {
        try {
            return ResponseEntity.ok(service.actualizar(id, corte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
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
    public List<Corte> listarPorRangoLargo(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        return service.listarPorRangoLargo(min, max);
    }

    @GetMapping("/precio")
    public List<Corte> listarPorRangoPrecio(@RequestParam BigDecimal min, @RequestParam BigDecimal max) {
        return service.listarPorRangoPrecio(min, max);
    }

    @GetMapping("/observaciones")
    public List<Corte> listarConObservaciones() {
        return service.listarConObservaciones();
    }
}