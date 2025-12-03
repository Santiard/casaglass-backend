package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.InventarioActualizarDTO;
import com.casaglass.casaglass_backend.dto.InventarioProductoDTO;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.service.InventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
// CORS configurado globalmente en CorsConfig.java
public class InventarioController {

    private final InventarioService service;

    public InventarioController(InventarioService service) {
        this.service = service;
    }

    // Listado general o filtrado
    // /api/inventario
    // /api/inventario?productoId=10
    // /api/inventario?sedeId=2
    // /api/inventario?productoId=10&sedeId=2
    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long productoId,
                                    @RequestParam(required = false) Long sedeId) {
        if (productoId != null && sedeId != null) {
            return service.obtenerPorProductoYSede(productoId, sedeId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        if (productoId != null) return ResponseEntity.ok(service.listarPorProducto(productoId));
        if (sedeId != null) return ResponseEntity.ok(service.listarPorSede(sedeId));
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventario> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear inventario: Body debe traer producto.id, sede.id y cantidad
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Inventario inventario) {
        try {
            return ResponseEntity.ok(service.guardar(inventario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Actualizar inventario por id
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Inventario inventario) {
        try {
            return ResponseEntity.ok(service.actualizar(id, inventario));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agrupado")
    public ResponseEntity<List<InventarioProductoDTO>> listarAgrupado() {
       return ResponseEntity.ok(service.listarInventarioAgrupado());
    }

    /**
     * 📦 ACTUALIZAR INVENTARIO DE UN PRODUCTO EN LAS 3 SEDES
     * Actualiza el inventario en Insula, Centro y Patios con los valores enviados
     * 
     * @param productoId ID del producto
     * @param dto DTO con las cantidades para las 3 sedes (cantidadInsula, cantidadCentro, cantidadPatios)
     * @return Lista de inventarios actualizados
     */
    @PutMapping("/producto/{productoId}")
    public ResponseEntity<?> actualizarInventarioPorProducto(
            @PathVariable Long productoId,
            @RequestBody InventarioActualizarDTO dto) {
        try {
            List<Inventario> inventarios = service.actualizarInventarioPorProducto(productoId, dto);
            return ResponseEntity.ok(inventarios);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar inventario: " + e.getMessage());
        }
    }
}