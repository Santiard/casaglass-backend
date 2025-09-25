package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.InventarioCorte;
import com.casaglass.casaglass_backend.service.InventarioCorteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario-cortes")
@CrossOrigin(origins = "*")
public class InventarioCorteController {

    private final InventarioCorteService service;

    public InventarioCorteController(InventarioCorteService service) {
        this.service = service;
    }

    // Listado general con filtros opcionales
    @GetMapping
    public List<InventarioCorte> listar(@RequestParam(required = false) Long corteId,
                                        @RequestParam(required = false) Long sedeId,
                                        @RequestParam(required = false, defaultValue = "false") boolean soloConStock,
                                        @RequestParam(required = false, defaultValue = "false") boolean soloSinStock,
                                        @RequestParam(required = false) Integer cantidadMin,
                                        @RequestParam(required = false) Integer cantidadMax) {
        
        // Filtros específicos
        if (soloConStock && corteId != null) {
            return service.listarPorCorteConStock(corteId);
        }
        
        if (soloConStock && sedeId != null) {
            return service.listarPorSedeConStock(sedeId);
        }
        
        if (soloConStock) {
            return service.listarConStock();
        }
        
        if (soloSinStock) {
            return service.listarSinStock();
        }
        
        if (cantidadMin != null && cantidadMax != null) {
            return service.listarPorRangoCantidad(cantidadMin, cantidadMax);
        }
        
        if (corteId != null) {
            return service.listarPorCorte(corteId);
        }
        
        if (sedeId != null) {
            return service.listarPorSede(sedeId);
        }
        
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventarioCorte> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/corte/{corteId}/sede/{sedeId}")
    public ResponseEntity<InventarioCorte> obtenerPorCorteYSede(@PathVariable Long corteId, @PathVariable Long sedeId) {
        return service.obtenerPorCorteYSede(corteId, sedeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<InventarioCorte> crear(@RequestBody InventarioCorte inventarioCorte) {
        try {
            return ResponseEntity.ok(service.guardar(inventarioCorte));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventarioCorte> actualizar(@PathVariable Long id, @RequestBody InventarioCorte inventarioCorte) {
        try {
            return ResponseEntity.ok(service.actualizar(id, inventarioCorte));
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
    @GetMapping("/corte/{corteId}")
    public List<InventarioCorte> listarPorCorte(@PathVariable Long corteId) {
        return service.listarPorCorte(corteId);
    }

    @GetMapping("/sede/{sedeId}")
    public List<InventarioCorte> listarPorSede(@PathVariable Long sedeId) {
        return service.listarPorSede(sedeId);
    }

    @GetMapping("/con-stock")
    public List<InventarioCorte> listarConStock() {
        return service.listarConStock();
    }

    @GetMapping("/sin-stock")
    public List<InventarioCorte> listarSinStock() {
        return service.listarSinStock();
    }

    @GetMapping("/corte/{corteId}/con-stock")
    public List<InventarioCorte> listarPorCorteConStock(@PathVariable Long corteId) {
        return service.listarPorCorteConStock(corteId);
    }

    @GetMapping("/sede/{sedeId}/con-stock")
    public List<InventarioCorte> listarPorSedeConStock(@PathVariable Long sedeId) {
        return service.listarPorSedeConStock(sedeId);
    }

    // Endpoints para obtener totales
    @GetMapping("/stock/corte/{corteId}")
    public ResponseEntity<Integer> obtenerStockTotalPorCorte(@PathVariable Long corteId) {
        return ResponseEntity.ok(service.obtenerStockTotalPorCorte(corteId));
    }

    @GetMapping("/stock/sede/{sedeId}")
    public ResponseEntity<Integer> obtenerStockTotalPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(service.obtenerStockTotalPorSede(sedeId));
    }

    @GetMapping("/sedes-con-stock/corte/{corteId}")
    public List<Long> obtenerSedesConStock(@PathVariable Long corteId) {
        return service.obtenerSedesConStock(corteId);
    }

    // Endpoints para manejo de stock
    @PutMapping("/stock/corte/{corteId}/sede/{sedeId}")
    public ResponseEntity<InventarioCorte> actualizarStock(@PathVariable Long corteId, 
                                                           @PathVariable Long sedeId, 
                                                           @RequestParam Integer cantidad) {
        try {
            return ResponseEntity.ok(service.actualizarStock(corteId, sedeId, cantidad));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/stock/incrementar/corte/{corteId}/sede/{sedeId}")
    public ResponseEntity<InventarioCorte> incrementarStock(@PathVariable Long corteId, 
                                                            @PathVariable Long sedeId, 
                                                            @RequestParam Integer cantidad) {
        try {
            return ResponseEntity.ok(service.incrementarStock(corteId, sedeId, cantidad));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/stock/decrementar/corte/{corteId}/sede/{sedeId}")
    public ResponseEntity<InventarioCorte> decrementarStock(@PathVariable Long corteId, 
                                                            @PathVariable Long sedeId, 
                                                            @RequestParam Integer cantidad) {
        try {
            return ResponseEntity.ok(service.decrementarStock(corteId, sedeId, cantidad));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}